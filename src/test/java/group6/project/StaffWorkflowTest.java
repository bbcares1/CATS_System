// We check the employee application, history and claim pages together.
package group6.project;

import static group6.project.TestRequests.multipart;
import static group6.project.TestRequests.post;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@Transactional
class StaffWorkflowTest {
    @Autowired WebApplicationContext context;
    @Autowired StaffRepo staffRepo;
    @Autowired TrainingEntitlementRepo entitlements;
    @Autowired UserRepo userRepo;
    @Autowired CourseApplicationRepo applicationRepo;
    @Autowired ExcludedDaysRepo excludedDaysRepo;
    @Autowired CourseFeeApplicationRepo claimRepo;
    @Autowired TrainingEntitlementService allowances;
    @Autowired CourseApplicationService applications;
    @Autowired CourseFeeApplicationService claimService;
    MockMvc mvc;
    Staff staff;
    MockHttpSession session;

    // Test setup: Create an employee and login session before each test.
    @BeforeEach
    void prepare() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        Manager reviewer = new Manager();
        reviewer.setUserName("reviewer");
        reviewer.setName("Reviewer");
        reviewer.setPassword("demo123");
        reviewer = userRepo.saveAndFlush(reviewer);
        staff = new Staff();
        staff.setManager(reviewer);
        staff.setUserName("ryan");
        staff.setPassword("test-password");
        staff.setName("Ryan");
        staff.setEmail("ryan@example.test");
        staff.setStaffId("S001");
        staff.setDesignation("Professional");
        staff.setRole(Roles.STAFF);
        staff = staffRepo.saveAndFlush(staff);
        allocate(staff, 10, 2000);
        session = new MockHttpSession();
        session.setAttribute("user", staff);
    }

    LocalDate futureDay() {
        LocalDate day = LocalDate.now().plusDays(7);
        while (day.getDayOfWeek().getValue() > 5) day = day.plusDays(1);
        return day;
    }

    CourseApplication form() {
        CourseApplication course = new CourseApplication();
        course.setCourseTitle("Java EE");
        course.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);
        course.setTrainingProvider("NUS-ISS");
        course.setCourseStartDate(futureDay());
        course.setCourseEndDate(futureDay());
        course.setCourseFee(new java.math.BigDecimal("300"));
        course.setJustification("Use Java EE to build web applications");
        course.setHalfDayPeriod("");
        return course;
    }

    CourseApplication saved(ApplicationStatus status, LocalDate date) {
        CourseApplication course = form();
        course.setApplicant(staff);
        course.setStatus(status);
        course.setCourseStartDate(date);
        course.setCourseEndDate(date);
        course.setTrainingDays(1d);
        return applicationRepo.saveAndFlush(course);
    }

    // Login and staff pages: Check that login and page rendering work.
    @Test
    void teamLoginAndEveryStaffPageRender() throws Exception {
        mvc.perform(
                        post("/employee/login")
                                .param("userName", "ryan")
                                .param("password", "test-password")
                                .param("designation", "Staff"))
                .andExpect(redirectedUrl("/staff/home"))
                .andExpect(request().sessionAttribute("user", staff));
        mvc.perform(
                        post("/employee/login")
                                .param("userName", "ryan")
                                .param("password", "wrong")
                                .param("designation", "Staff"))
                .andExpect(view().name("employee-login"))
                .andExpect(model().attributeExists("error"));
        for (String path :
                List.of("/staff/home", "/staff/personal", "/staff/apply/other", "/staff/fee")) {
            mvc.perform(get(path).session(session)).andExpect(status().isOk());
            mvc.perform(get(path)).andExpect(redirectedUrl("/employee/login"));
        }
        CourseApplication course = saved(ApplicationStatus.APPLIED, futureDay());
        mvc.perform(get("/staff/personal").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Java EE")));
        mvc.perform(get("/staff/applications/" + course.getCourseId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Java EE")));
        mvc.perform(get("/staff/applications/" + course.getCourseId() + "/edit").session(session))
                .andExpect(status().isOk());
        mvc.perform(post("/logout").session(session)).andExpect(redirectedUrl("/login"));
        assertTrue(session.isInvalid());
    }

    // Manager access: Check that the team dashboard and staff functions still work.
    @Test
    void managerCanStillUseTeamDashboardAndStaffFunctions() throws Exception {
        Manager manager = new Manager();
        manager.setUserName("junie");
        manager.setPassword("test-password");
        manager.setName("Junie");
        manager.setEmail("junie@example.test");
        manager.setRole(Roles.MANAGER);
        manager.setStaffId("S002");
        staffRepo.saveAndFlush(manager);
        MockHttpSession managerSession = new MockHttpSession();
        mvc.perform(
                        post("/employee/login")
                                .session(managerSession)
                                .param("userName", "junie")
                                .param("password", "test-password")
                                .param("designation", "Manager"))
                .andExpect(redirectedUrl("/manager/home"));
        mvc.perform(get("/manager/home").session(managerSession)).andExpect(status().isOk());
        mvc.perform(get("/staff/home").session(managerSession)).andExpect(status().isOk());
        assertInstanceOf(Staff.class, managerSession.getAttribute("user"));
        assertInstanceOf(User.class, managerSession.getAttribute("user"));
    }

    // Form binding: Check that submitted fields cannot replace the employee or status.
    @Test
    void formBindingDoesNotAllowIdentityOrStatusChanges() throws Exception {
        mvc.perform(
                        post("/staff/apply/other")
                                .session(session)
                                .param("courseTitle", "Design 1")
                                .param("courseCategory", "EXTERNAL_COURSE")
                                .param("courseStartDate", futureDay().toString())
                                .param("courseEndDate", futureDay().toString())
                                .param("trainingProvider", "Demo provider")
                                .param("courseFee", "100")
                                .param("justification", "Improve software design skills")
                                .param("courseId", "999999")
                                .param("status", "APPROVED")
                                .param("applicant.userId", "999999"))
                .andExpect(status().is3xxRedirection());
        CourseApplication course =
                applications.findForStaffAndYear(staff, futureDay().getYear()).getFirst();
        assertEquals(ApplicationStatus.APPLIED, course.getStatus());
        assertEquals(staff.getUserId(), course.getApplicant().getUserId());
        assertNotEquals(999999, course.getCourseId());
        mvc.perform(post("/staff/apply/other").session(session).param("courseFee", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("staff-application-form"))
                .andExpect(model().attributeHasErrors("form"));
    }

    // Application validation: Show an error without saving an invalid form.
    @Test
    void invalidFormShowsAnErrorWithoutSaving() throws Exception {
        long before = applicationRepo.count();
        mvc.perform(post("/staff/apply/other").session(session).param("courseFee", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("staff-application-form"))
                .andExpect(model().attributeHasFieldErrors("form", "courseFee"));

        mvc.perform(
                        post("/staff/apply/other")
                                .session(session)
                                .param("courseTitle", " ")
                                .param("courseFee", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("staff-application-form"))
                .andExpect(model().attributeHasFieldErrors("form", "courseTitle"));
        assertEquals(before, applicationRepo.count());
    }

    // Course history: Check application status changes and personal history.
    @Test
    void lifecycleAndOwnCurrentYearHistory() throws Exception {
        CourseApplication course = saveApplication(null, form(), staff);
        assertEquals(ApplicationStatus.APPLIED, course.getStatus());
        CourseApplication edit = form();
        edit.setCourseTitle("Java EE 2");
        saveApplication(course.getCourseId(), edit, staff);
        assertEquals(ApplicationStatus.UPDATED, course.getStatus());
        mvc.perform(
                        post("/staff/applications/" + course.getCourseId() + "/delete")
                                .session(session)
                                .param("version", version(course.getCourseId()).toString()))
                .andExpect(redirectedUrl("/staff/applications/" + course.getCourseId()));
        assertEquals(ApplicationStatus.DELETED, course.getStatus());
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(course.getCourseId(), form(), staff));
        CourseApplication approved = saved(ApplicationStatus.APPROVED, futureDay());
        mvc.perform(
                        post("/staff/applications/" + approved.getCourseId() + "/cancel")
                                .session(session)
                                .param("version", version(approved.getCourseId()).toString()))
                .andExpect(redirectedUrl("/staff/applications/" + approved.getCourseId()));
        assertEquals(ApplicationStatus.CANCELLED, approved.getStatus());
        CourseApplication ended = saved(ApplicationStatus.APPROVED, LocalDate.now().minusDays(2));
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> completeApplication(ended.getCourseId(), " ", staff));
        mvc.perform(
                        post("/staff/applications/" + ended.getCourseId() + "/complete")
                                .session(session)
                                .param("version", version(ended.getCourseId()).toString())
                                .param(
                                        "experienceComments",
                                        "Learned to build Java web applications"))
                .andExpect(redirectedUrl("/staff/applications/" + ended.getCourseId()));
        assertEquals(ApplicationStatus.COMPLETED, ended.getStatus());
        assertEquals("Learned to build Java web applications", ended.getExperienceComments());
        assertEquals(1, allowances.summary(staff, LocalDate.now().getYear(), null).usedDays());
        assertEquals(
                0,
                new java.math.BigDecimal("300")
                        .compareTo(
                                allowances
                                        .summary(staff, LocalDate.now().getYear(), null)
                                        .usedBudget()));
        saved(ApplicationStatus.COMPLETED, LocalDate.now().minusYears(1));
        for (CourseApplication row :
                applications.findForStaffAndYear(staff, LocalDate.now().getYear())) {
            assertEquals(LocalDate.now().getYear(), row.getCourseStartDate().getYear());
        }
        Staff another = new Staff();
        another.setUserId(staff.getUserId() + 1);
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> applications.getOwned(ended.getCourseId(), another));
    }

    // Manual completion and claims use the same flow before, on or after the course end date.
    @ParameterizedTest
    @ValueSource(ints = {-2, 0, 7})
    void manualCompletionAllowsAnyApprovedCourseAndAClaim(int daysAhead) throws Exception {
        LocalDate date = LocalDate.now().plusDays(daysAhead);
        CourseApplication course = saved(ApplicationStatus.APPROVED, date);
        String path = "/staff/applications/" + course.getCourseId();
        var before = allowances.summary(staff, LocalDate.now().getYear(), null);
        mvc.perform(get(path).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Mark completed</button>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("complete-demo"))));
        mvc.perform(
                        post(path + "/complete")
                                .session(session)
                                .param("version", version(course.getCourseId()).toString())
                                .param("experienceComments", "  Practised the completion and claim flow.  "))
                .andExpect(redirectedUrl(path))
                .andExpect(flash().attribute("success", "Application updated."));
        applicationRepo.flush();
        assertEquals(ApplicationStatus.COMPLETED, course.getStatus());
        assertEquals("Practised the completion and claim flow.", course.getExperienceComments());
        assertEquals(date, course.getCourseStartDate());
        assertEquals(date, course.getCourseEndDate());
        mvc.perform(get(path).session(session))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Practised the completion")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Mark completed</button>"))));
        mvc.perform(get("/staff/fee").session(session))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("accept="))));
        mvc.perform(
                        multipart("/staff/fee")
                                .file(new MockMultipartFile(
                                        "receipt", "receipt.txt", "text/plain", "Demo receipt".getBytes()))
                                .file(new MockMultipartFile(
                                        "certificate", "certificate.docx", "application/octet-stream",
                                        "Demo certificate".getBytes()))
                                .session(session)
                                .param("courseId", course.getCourseId().toString())
                                .param("paidPersonally", "true"))
                .andExpect(redirectedUrl("/staff/fee"));
        assertTrue(claimRepo.existsByCourseApplication_CourseId(course.getCourseId()));
        var after = allowances.summary(staff, LocalDate.now().getYear(), null);
        assertEquals(before.usedDays(), after.usedDays());
        assertEquals(0, before.usedBudget().compareTo(after.usedBudget()));
    }

    @ParameterizedTest
    @EnumSource(
            value = ApplicationStatus.class,
            names = "APPROVED",
            mode = EnumSource.Mode.EXCLUDE)
    void manualCompletionRequiresApproval(ApplicationStatus state) throws Exception {
        CourseApplication course = saved(state, futureDay());
        String path = "/staff/applications/" + course.getCourseId();
        mvc.perform(get(path).session(session))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Mark completed</button>"))));
        mvc.perform(
                        post(path + "/complete")
                                .session(session)
                                .param("version", version(course.getCourseId()).toString())
                                .param("experienceComments", "Attendance comments"))
                .andExpect(redirectedUrl(path))
                .andExpect(flash().attribute(
                        "error", "Only approved courses can be completed."));
        assertEquals(state, course.getStatus());
        assertNull(course.getExperienceComments());
    }

    @Test
    void manualCompletionRequiresNotesAndKeepsInvalidInput() throws Exception {
        CourseApplication course = saved(ApplicationStatus.APPROVED, futureDay());
        String path = "/staff/applications/" + course.getCourseId();
        for (String comments : List.of(" ", "x".repeat(2001))) {
            mvc.perform(
                            post(path + "/complete")
                                    .session(session)
                                    .param("version", version(course.getCourseId()).toString())
                                    .param("experienceComments", comments))
                    .andExpect(redirectedUrl(path))
                    .andExpect(flash().attributeExists("error"))
                    .andExpect(flash().attribute("experienceComments", comments));
            assertEquals(ApplicationStatus.APPROVED, course.getStatus());
            assertNull(course.getExperienceComments());
        }
        applications.complete(course.getCourseId(), version(course.getCourseId()), "x".repeat(2000), staff);
        applicationRepo.flush();
        assertEquals(2000, course.getExperienceComments().length());
    }

    // Another owner, stale forms and repeat completion must still be rejected.
    @Test
    void manualCompletionChecksOwnerVersionAndRepeatRequests() throws Exception {
        CourseApplication course = saved(ApplicationStatus.APPROVED, futureDay());
        String path = "/staff/applications/" + course.getCourseId();
        Long originalVersion = version(course.getCourseId());
        MockHttpSession otherSession = new MockHttpSession();
        otherSession.setAttribute("user", staff.getManager());
        mvc.perform(
                        post(path + "/complete")
                                .session(otherSession)
                                .param("version", originalVersion.toString())
                                .param("experienceComments", "Attendance comments"))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post(path + "/complete")
                                .session(session)
                                .param("version", Long.toString(originalVersion + 1))
                                .param("experienceComments", "Attendance comments"))
                .andExpect(status().isConflict());
        assertEquals(ApplicationStatus.APPROVED, course.getStatus());
        applications.complete(course.getCourseId(), originalVersion, "Attendance comments", staff);
        applicationRepo.flush();
        mvc.perform(
                        post(path + "/complete")
                                .session(session)
                                .param("version", originalVersion.toString())
                                .param("experienceComments", "Changed"))
                .andExpect(status().isConflict());
        mvc.perform(
                        post(path + "/complete")
                                .session(session)
                                .param("version", version(course.getCourseId()).toString())
                                .param("experienceComments", "Changed"))
                .andExpect(flash().attributeExists("error"));
        assertEquals("Attendance comments", course.getExperienceComments());
    }

    // Application rules: Check dates, holidays, budget and overlapping courses.
    @Test
    void datesHolidaysBudgetAndOverlapsAreValidated() {
        CourseApplication course = form();
        course.setCourseStartDate(LocalDate.now());
        course.setCourseEndDate(LocalDate.now());
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(null, course, staff));
        course.setCourseStartDate(futureDay());
        course.setCourseEndDate(futureDay().minusDays(1));
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(null, course, staff));
        course.setCourseEndDate(futureDay());
        course.setCourseFee(new java.math.BigDecimal("2001"));
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(null, course, staff));
        course.setCourseFee(null);
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(null, course, staff));
        course.setCourseFee(new java.math.BigDecimal("300"));
        course.setHalfDayPeriod("AM");
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(null, course, staff));
        course.setHalfDayPeriod("");
        ExcludedDays holiday = new ExcludedDays();
        holiday.setDate(futureDay());
        holiday.setDescription("Company Holiday");
        excludedDaysRepo.saveAndFlush(holiday);
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(null, course, staff));
        excludedDaysRepo.delete(holiday);
        excludedDaysRepo.flush();
        saved(ApplicationStatus.APPLIED, futureDay());
        course.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING);
        course.setHalfDayPeriod("AM");
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(null, course, staff));
    }

    // Training days: Check working days and separate AM and PM sessions.
    @Test
    void workingDayCalculationAndOppositeHalfDays() {
        LocalDate monday = futureDay();
        while (monday.getDayOfWeek().getValue() != 1) monday = monday.plusDays(1);
        ExcludedDays holiday = new ExcludedDays();
        holiday.setDate(monday.plusDays(2));
        holiday.setDescription("Company Holiday");
        excludedDaysRepo.saveAndFlush(holiday);
        CourseApplication period = form();
        period.setCourseStartDate(monday);
        period.setCourseEndDate(monday.plusDays(7));
        period = saveApplication(null, period, staff);
        assertEquals(5d, period.getTrainingDays());
        deleteApplication(period.getCourseId(), staff);
        CourseApplication morning = form();
        morning.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING);
        morning.setCourseStartDate(monday);
        morning.setCourseEndDate(monday);
        morning.setHalfDayPeriod("AM");
        morning = saveApplication(null, morning, staff);
        assertEquals(0.5, morning.getTrainingDays());
        assertEquals(0, morning.getCourseFee().signum());
        CourseApplication afternoon = form();
        afternoon.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING);
        afternoon.setCourseStartDate(monday);
        afternoon.setCourseEndDate(monday);
        afternoon.setHalfDayPeriod("PM");
        afternoon = saveApplication(null, afternoon, staff);
        assertEquals(0.5, afternoon.getTrainingDays());
        CourseApplication fullDay = form();
        fullDay.setCourseStartDate(monday);
        fullDay.setCourseEndDate(monday);
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(null, fullDay, staff));
        deleteApplication(morning.getCourseId(), staff);
        deleteApplication(afternoon.getCourseId(), staff);
        CourseApplication training = form();
        training.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING);
        training.setCourseStartDate(monday);
        training.setCourseEndDate(monday.plusDays(1));
        training.setHalfDayPeriod("AM");
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(null, training, staff));
        training.setHalfDayPeriod(null);
        assertEquals(2d, saveApplication(null, training, staff).getTrainingDays());
        CourseApplication overlapping = form();
        overlapping.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING);
        overlapping.setCourseStartDate(monday);
        overlapping.setCourseEndDate(monday);
        overlapping.setHalfDayPeriod("PM");
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(null, overlapping, staff));
    }

    // Fee claims: Check course completion, documents and the manager decision.
    @Test
    void feeClaimRequiresCompletedCourseAndDocumentsAndShowsManagerReason() throws Exception {
        CourseApplication course = saved(ApplicationStatus.COMPLETED, LocalDate.now().minusDays(2));
        course.setCourseTitle("AWS Cloud Practitioner Essentials");
        course.setTrainingProvider("AWS");
        byte[] pdf = new byte[100000];
        System.arraycopy("%PDF-1.4".getBytes(), 0, pdf, 0, 8);
        MockMultipartFile receipt =
                new MockMultipartFile("receipt", "receipt.pdf", "application/pdf", pdf);
        MockMultipartFile certificate =
                new MockMultipartFile("certificate", "certificate.pdf", "application/pdf", pdf);
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () ->
                        claimService.submit(
                                course.getCourseId(), false, receipt, certificate, staff, null));
        mvc.perform(
                        multipart("/staff/fee")
                                .file(receipt)
                                .file(certificate)
                                .session(session)
                                .param("courseId", course.getCourseId().toString())
                                .param("paidPersonally", "true"))
                .andExpect(redirectedUrl("/staff/fee"));
        claimRepo.flush();
        CourseFeeApplication claim = claimRepo.findByApplicant_UserId(staff.getUserId()).getFirst();
        assertEquals(ApplicationStatus.APPLIED, claim.getApplicationStatus());
        assertEquals("receipt.pdf", claim.getReceiptFileName());
        assertEquals("application/pdf", claim.getReceiptContentType());
        assertEquals("certificate.pdf", claim.getCertificateFileName());
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () ->
                        claimService.submit(
                                course.getCourseId(), true, receipt, certificate, staff, null));
        claimService.decide(
                claim.getApplicationId(),
                staff.getManager().getUserId(),
                "approve",
                "Receipt verified",
                claim.getVersion());
        mvc.perform(get("/staff/claims/" + claim.getApplicationId()).session(session))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("Receipt verified")));
        mvc.perform(get("/staff/claims/" + claim.getApplicationId() + "/receipt").session(session))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdf))
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        org.hamcrest.Matchers.containsString("receipt.pdf")));
        mvc.perform(get("/staff/fee").session(session)).andExpect(status().isOk());
    }

    // History keeps the selected year while paging and changing the result count.
    @Test
    void paginationRenders() throws Exception {
        int year = LocalDate.now().getYear();
        for (int i = 0; i < 7; i++) saved(ApplicationStatus.DELETED, LocalDate.now());
        saved(ApplicationStatus.DELETED, LocalDate.now().minusYears(1));
        var first =
                mvc.perform(get("/staff/personal").session(session))
                        .andExpect(status().isOk())
                        .andExpect(
                                model().attribute("applications", org.hamcrest.Matchers.hasSize(5)))
                        .andReturn();
        var firstPage = (Page<?>) first.getModelAndView().getModel().get("pageData");
        assertEquals(5, firstPage.getSize());
        assertEquals(7, firstPage.getTotalElements());
        assertTrue(firstPage.hasNext());
        String html = first.getResponse().getContentAsString();
        for (int size : List.of(5, 10, 15)) assertTrue(html.contains("value=\"" + size + "\""));
        assertFalse(html.contains("value=\"20\""));
        assertFalse(html.contains("value=\"25\""));
        mvc.perform(
                        get("/staff/personal")
                                .session(session)
                                .param("year", String.valueOf(year))
                                .param("page", "1")
                                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("year", year))
                .andExpect(model().attribute("applications", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "/staff/personal?year="
                                                        + year
                                                        + "&amp;page=0&amp;size=5")));
        for (int size : List.of(10, 15)) {
            var result =
                    mvc.perform(
                                    get("/staff/personal")
                                            .session(session)
                                            .param("year", String.valueOf(year))
                                            .param("size", String.valueOf(size)))
                            .andExpect(status().isOk())
                            .andExpect(
                                    model().attribute(
                                                    "applications",
                                                    org.hamcrest.Matchers.hasSize(7)))
                            .andReturn();
            var page = (Page<?>) result.getModelAndView().getModel().get("pageData");
            assertEquals(size, page.getSize());
            assertEquals(0, page.getNumber());
        }
        var invalid =
                mvc.perform(
                                get("/staff/personal")
                                        .session(session)
                                        .param("page", "-1")
                                        .param("size", "20"))
                        .andExpect(status().isOk())
                        .andExpect(
                                model().attribute("applications", org.hamcrest.Matchers.hasSize(5)))
                        .andReturn();
        var page = (Page<?>) invalid.getModelAndView().getModel().get("pageData");
        assertEquals(5, page.getSize());
        assertEquals(0, page.getNumber());
        mvc.perform(get("/staff/personal").session(session).param("page", "99"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("applications", org.hamcrest.Matchers.hasSize(2)));
        mvc.perform(get("/staff/personal").session(session).param("year", String.valueOf(year - 2)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("applications", org.hamcrest.Matchers.empty()));
    }

    // Allowance and claims: Check completed course totals and invalid claim input.
    @Test
    void completedCoursesConsumeAllowanceAndClaimValidationRejectsInvalidInput() {
        saved(ApplicationStatus.COMPLETED, LocalDate.now().minusDays(2));
        allocate(staff, 1, 2000);
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(null, form(), staff));
        allocate(staff, 10, 500);
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> saveApplication(null, form(), staff));
        CourseApplication pending = saved(ApplicationStatus.APPLIED, futureDay());
        MockMultipartFile bad =
                new MockMultipartFile("receipt", "empty.pdf", "application/pdf", new byte[0]);
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> claimService.submit(pending.getCourseId(), true, bad, bad, staff, null));
        CourseApplication completed =
                applications.findForStaffAndYear(staff, LocalDate.now().getYear()).getFirst();
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> claimService.submit(completed.getCourseId(), true, bad, bad, staff, null));
        assertTrue(claimRepo.findByApplicant_UserId(staff.getUserId()).isEmpty());
    }

    // Course fee: Allow an external course with no fee.
    @Test
    void externalCourseCanHaveZeroFee() {
        CourseApplication course = form();
        course.setCourseFee(new java.math.BigDecimal("0"));
        assertEquals(0, saveApplication(null, course, staff).getCourseFee().signum());
    }

    // Reporting manager: Save the relationship and find the correct staff.
    @Test
    void reportingManagerPersistsAndFinderReturnsOnlyTheirStaff() throws Exception {
        Manager manager = new Manager();
        manager.setUserName("michael");
        manager.setPassword("test-password");
        manager.setName("Michael");
        manager.setEmail("michael@example.test");
        manager.setRole(Roles.MANAGER);
        manager.setStaffId("S003");
        staffRepo.saveAndFlush(manager);
        staff.setManager(manager);
        staffRepo.saveAndFlush(staff);

        Staff second = new Staff();
        second.setUserName("owen");
        second.setPassword("test-password");
        second.setName("Owen");
        second.setEmail("owen@example.test");
        second.setStaffId("S004");
        second.setRole(Roles.STAFF);
        second.setManager(manager);
        staffRepo.saveAndFlush(second);

        // Staff directory: Include the other group members without assigning a manager.
        Staff hongfan = new Staff();
        hongfan.setUserName("hongfan");
        hongfan.setName("Hongfan");
        hongfan.setEmail("hongfan@example.test");
        hongfan.setStaffId("S006");
        hongfan.setPassword("test-password");
        hongfan.setRole(Roles.STAFF);
        staffRepo.saveAndFlush(hongfan);

        Staff jialu = new Staff();
        jialu.setUserName("jialu");
        jialu.setName("Jialu");
        jialu.setEmail("jialu@example.test");
        jialu.setStaffId("S007");
        jialu.setPassword("test-password");
        jialu.setRole(Roles.STAFF);
        staffRepo.saveAndFlush(jialu);

        Staff imran = new Staff();
        imran.setUserName("imran");
        imran.setName("Imran");
        imran.setEmail("imran@example.test");
        imran.setStaffId("S008");
        imran.setPassword("test-password");
        imran.setRole(Roles.STAFF);
        staffRepo.saveAndFlush(imran);

        Integer staffId = staff.getUserId();
        Integer managerId = manager.getUserId();
        context.getBean(jakarta.persistence.EntityManager.class).clear();
        Staff loaded = staffRepo.findById(staffId).orElseThrow();
        assertEquals(managerId, loaded.getManager().getUserId());
        assertInstanceOf(Manager.class, loaded.getManager());
        assertEquals(2, staffRepo.findByManager_UserId(managerId).size());
        assertTrue(staffRepo.findByManager_UserId(-1).isEmpty());
        assertNull(staffRepo.findById(managerId).orElseThrow().getManager());
        session.setAttribute("user", loaded);
        mvc.perform(get("/staff/home").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Michael")));
    }

    // Staff workflow: Check status rules and personal lists.
    @Test
    void staffStatusRulesAndPersonalListsArePreserved() throws Exception {
        for (ApplicationStatus state :
                List.of(
                        ApplicationStatus.REJECTED,
                        ApplicationStatus.CANCELLED,
                        ApplicationStatus.DELETED,
                        ApplicationStatus.COMPLETED)) {
            CourseApplication course = saved(state, LocalDate.now().minusDays(3));
            assertThrows(
                    org.springframework.web.server.ResponseStatusException.class,
                    () -> saveApplication(course.getCourseId(), form(), staff));
            assertThrows(
                    org.springframework.web.server.ResponseStatusException.class,
                    () -> deleteApplication(course.getCourseId(), staff));
            assertThrows(
                    org.springframework.web.server.ResponseStatusException.class,
                    () -> cancelApplication(course.getCourseId(), staff));
            mvc.perform(get("/staff/applications/" + course.getCourseId()).session(session))
                    .andExpect(status().isOk());
        }
        Staff second = new Staff();
        second.setUserName("martin");
        second.setPassword("test-password");
        second.setName("Martin");
        second.setEmail("martin@example.test");
        second.setRole(Roles.STAFF);
        second.setStaffId("S005");
        staffRepo.saveAndFlush(second);
        CourseApplication otherCourse = form();
        otherCourse.setApplicant(second);
        applicationRepo.saveAndFlush(otherCourse);
        for (CourseApplication row :
                applications.findForStaffAndYear(staff, LocalDate.now().getYear())) {
            assertEquals(staff.getUserId(), row.getApplicant().getUserId());
        }
        mvc.perform(get("/staff/applications/" + otherCourse.getCourseId()).session(session))
                .andExpect(status().isNotFound());
        mvc.perform(
                        get("/staff/applications/" + otherCourse.getCourseId() + "/edit")
                                .session(session))
                .andExpect(status().isNotFound());
        CourseFeeApplication otherClaim = new CourseFeeApplication();
        otherClaim.setApplicant(second);
        otherClaim.setApplicationStatus(ApplicationStatus.APPLIED);
        otherClaim.setReceipt(new byte[] {1});
        otherClaim.setCertificate(new byte[] {1});
        otherClaim.setCourseApplication(otherCourse);
        otherClaim = claimRepo.saveAndFlush(otherClaim);
        mvc.perform(get("/staff/claims/" + otherClaim.getApplicationId()).session(session))
                .andExpect(status().isNotFound());
        for (String document : List.of("receipt", "certificate")) {
            mvc.perform(
                            get("/staff/claims/" + otherClaim.getApplicationId() + "/" + document)
                                    .session(session))
                    .andExpect(status().isNotFound());
            mvc.perform(get("/staff/claims/" + otherClaim.getApplicationId() + "/" + document))
                    .andExpect(status().isUnauthorized());
        }
    }

    // Each fixture owns one allowance row for this year; other years are independent.
    private void allocate(User employee, double days, double budget) {
        int year = LocalDate.now().getYear();
        TrainingEntitlement allowance =
                entitlements
                        .findByStaff_UserIdAndYear(employee.getUserId(), year)
                        .orElse(new TrainingEntitlement(year));
        allowance.setStaff(employee);
        allowance.setDayLimit(days);
        allowance.setBudget(java.math.BigDecimal.valueOf(budget));
        entitlements.saveAndFlush(allowance);
    }

    // Keep existing workflow scenarios while submitting the new, restricted form type.
    private CourseApplication saveApplication(
            Integer id, CourseApplication source, Staff employee) {
        var form = new group6.project.form.CourseApplicationForm();
        form.setCourseTitle(source.getCourseTitle());
        form.setCourseCategory(source.getCourseCategory());
        form.setTrainingProvider(source.getTrainingProvider());
        form.setCourseFee(source.getCourseFee());
        form.setCourseStartDate(source.getCourseStartDate());
        form.setCourseEndDate(source.getCourseEndDate());
        form.setHalfDayPeriod(source.getHalfDayPeriod());
        form.setJustification(source.getJustification());
        form.setWorkDissemination(source.getWorkDissemination());
        if (id != null) form.setVersion(version(id));
        return id == null
                ? applications.createOther(form, employee)
                : applications.updateOther(id, form, employee);
    }

    private Long version(Integer id) {
        applicationRepo.flush();
        return applicationRepo.findById(id).orElseThrow().getVersion();
    }

    private void deleteApplication(Integer id, Staff employee) {
        applications.delete(id, version(id), employee);
    }

    private void cancelApplication(Integer id, Staff employee) {
        applications.cancel(id, version(id), employee);
    }

    private void completeApplication(Integer id, String comments, Staff employee) {
        applications.complete(id, version(id), comments, employee);
    }
}

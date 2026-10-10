package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.*;

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
    @Autowired StaffService staffService;
    @Autowired CourseFeeApplicationService claimService;
    MockMvc mvc;
    Staff staff;
    MockHttpSession session;

    // Test setup: Create an employee and login session before each test.
    @BeforeEach
    void prepare() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        staff = new Staff();
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
        mvc.perform(post("/employee/login").param("userName", "ryan")
                .param("password", "test-password").param("designation", "Staff"))
                .andExpect(redirectedUrl("/staff/home")).andExpect(request().sessionAttribute("user", staff));
        mvc.perform(post("/employee/login").param("userName", "ryan")
                .param("password", "wrong").param("designation", "Staff"))
                .andExpect(view().name("employee-login")).andExpect(model().attributeExists("error"));
        for (String path : List.of("/staff/home", "/staff/personal", "/staff/apply", "/staff/fee")) {
            mvc.perform(get(path).session(session)).andExpect(status().isOk());
            mvc.perform(get(path)).andExpect(redirectedUrl("/employee/login"));
        }
        CourseApplication course = saved(ApplicationStatus.APPLIED, futureDay());
        mvc.perform(get("/staff/personal").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Java EE")));
        mvc.perform(get("/staff/applications/" + course.getCourseId()).session(session))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Java EE")));
        mvc.perform(get("/staff/applications/" + course.getCourseId() + "/edit").session(session))
                .andExpect(status().isOk());
        mvc.perform(get("/logout").session(session)).andExpect(redirectedUrl("/login"));
        assertTrue(session.isInvalid());
    }

    // Manager access: Check that the team dashboard and staff functions still work.
    @Test
    void managerCanStillUseTeamDashboardAndStaffFunctions() throws Exception {
        Manager manager = new Manager();
        manager.setUserName("junie"); manager.setPassword("test-password");
        manager.setName("Junie"); manager.setEmail("junie@example.test"); manager.setRole(Roles.MANAGER);
        manager.setStaffId("S002");
        staffRepo.saveAndFlush(manager);
        MockHttpSession managerSession = new MockHttpSession();
        mvc.perform(post("/employee/login").session(managerSession).param("userName", "junie")
                .param("password", "test-password").param("designation", "Manager"))
                .andExpect(redirectedUrl("/manager/home"));
        mvc.perform(get("/manager/home").session(managerSession)).andExpect(status().isOk());
        mvc.perform(get("/staff/home").session(managerSession)).andExpect(status().isOk());
        assertInstanceOf(Staff.class, managerSession.getAttribute("user"));
        assertInstanceOf(User.class, managerSession.getAttribute("user"));
    }

    // Form binding: Check that submitted fields cannot replace the employee or status.
    @Test
    void formBindingDoesNotAllowIdentityOrStatusChanges() throws Exception {
        mvc.perform(post("/staff/applications/save").session(session)
                .param("courseTitle", "Design 1").param("courseCategory", "EXTERNAL_COURSE")
                .param("courseStartDate", futureDay().toString()).param("courseEndDate", futureDay().toString())
                .param("courseFee", "100").param("justification", "Improve software design skills")
                .param("courseId", "999999").param("status", "APPROVED").param("applicant.userId", "999999"))
                .andExpect(status().is3xxRedirection());
        CourseApplication course = staffService.getCourseHistory(staff, futureDay().getYear()).getFirst();
        assertEquals(ApplicationStatus.APPLIED, course.getStatus());
        assertEquals(staff.getUserId(), course.getApplicant().getUserId());
        assertNotEquals(999999, course.getCourseId());
        mvc.perform(post("/staff/applications/save").session(session).param("courseFee", "abc"))
                .andExpect(view().name("staff-application-form")).andExpect(model().attributeExists("error"));
    }

    // Application validation: Show an error without saving an invalid form.
    @Test
    void invalidFormShowsAnErrorWithoutSaving() throws Exception {
        long before = applicationRepo.count();
        mvc.perform(post("/staff/applications/save").session(session)
                .param("courseFee", "not-a-number"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff-application-form"))
                .andExpect(model().attributeHasFieldErrors("course", "courseFee"));

        mvc.perform(post("/staff/applications/save").session(session)
                .param("courseTitle", " ").param("courseFee", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff-application-form"))
                .andExpect(model().attribute("error", "Course title, category, dates and justification are required."))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Course title, category, dates and justification are required.")));
        assertEquals(before, applicationRepo.count());
    }

    // Course history: Check application status changes and personal history.
    @Test
    void lifecycleAndOwnCurrentYearHistory() throws Exception {
        CourseApplication course = staffService.saveApplication(null, form(), staff);
        assertEquals(ApplicationStatus.APPLIED, course.getStatus());
        CourseApplication edit = form(); edit.setCourseTitle("Java EE 2");
        staffService.saveApplication(course.getCourseId(), edit, staff);
        assertEquals(ApplicationStatus.UPDATED, course.getStatus());
        mvc.perform(post("/staff/applications/" + course.getCourseId() + "/delete").session(session))
                .andExpect(redirectedUrl("/staff/applications/" + course.getCourseId()));
        assertEquals(ApplicationStatus.DELETED, course.getStatus());
        assertThrows(IllegalStateException.class, () -> staffService.saveApplication(course.getCourseId(), form(), staff));
        CourseApplication approved = saved(ApplicationStatus.APPROVED, futureDay());
        assertThrows(IllegalStateException.class, () -> staffService.completeApplication(approved.getCourseId(), "Learned to build Java web applications", staff));
        mvc.perform(post("/staff/applications/" + approved.getCourseId() + "/cancel").session(session))
                .andExpect(redirectedUrl("/staff/applications/" + approved.getCourseId()));
        assertEquals(ApplicationStatus.CANCELLED, approved.getStatus());
        CourseApplication ended = saved(ApplicationStatus.APPROVED, LocalDate.now().minusDays(2));
        assertThrows(IllegalArgumentException.class, () -> staffService.completeApplication(ended.getCourseId(), " ", staff));
        mvc.perform(post("/staff/applications/" + ended.getCourseId() + "/complete").session(session)
                .param("experienceComments", "Learned to build Java web applications"))
                .andExpect(redirectedUrl("/staff/applications/" + ended.getCourseId()));
        assertEquals(ApplicationStatus.COMPLETED, ended.getStatus());
        assertEquals(1, staffService.summary(new CourseApplication(), staff, null).usedDays());
        assertEquals(0, new java.math.BigDecimal("300").compareTo(staffService.summary(new CourseApplication(), staff, null).usedBudget()));
        saved(ApplicationStatus.COMPLETED, LocalDate.now().minusYears(1));
        for (CourseApplication row : staffService.getCourseHistory(staff, LocalDate.now().getYear())) {
            assertEquals(LocalDate.now().getYear(), row.getCourseStartDate().getYear());
        }
        Staff another = new Staff(); another.setUserId(staff.getUserId() + 1);
        assertThrows(IllegalArgumentException.class, () -> staffService.getCourseApplication(ended.getCourseId(), another));
    }

    // Application rules: Check dates, holidays, budget and overlapping courses.
    @Test
    void datesHolidaysBudgetAndOverlapsAreValidated() {
        CourseApplication course = form();
        course.setCourseStartDate(LocalDate.now()); course.setCourseEndDate(LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> staffService.saveApplication(null, course, staff));
        course.setCourseStartDate(futureDay()); course.setCourseEndDate(futureDay().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> staffService.saveApplication(null, course, staff));
        course.setCourseEndDate(futureDay()); course.setCourseFee(new java.math.BigDecimal("2001"));
        assertThrows(IllegalArgumentException.class, () -> staffService.saveApplication(null, course, staff));
        course.setCourseFee(null);
        assertThrows(IllegalArgumentException.class, () -> staffService.saveApplication(null, course, staff));
        course.setCourseFee(new java.math.BigDecimal("300")); course.setHalfDayPeriod("AM");
        assertThrows(IllegalArgumentException.class, () -> staffService.saveApplication(null, course, staff));
        course.setHalfDayPeriod("");
        ExcludedDays holiday = new ExcludedDays(); holiday.setDate(futureDay()); holiday.setDescription("Company Holiday");
        excludedDaysRepo.saveAndFlush(holiday);
        assertThrows(IllegalArgumentException.class, () -> staffService.saveApplication(null, course, staff));
        excludedDaysRepo.delete(holiday); excludedDaysRepo.flush();
        saved(ApplicationStatus.APPLIED, futureDay());
        course.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING); course.setHalfDayPeriod("AM");
        assertThrows(IllegalArgumentException.class, () -> staffService.saveApplication(null, course, staff));
    }

    // Training days: Check working days and separate AM and PM sessions.
    @Test
    void workingDayCalculationAndOppositeHalfDays() {
        LocalDate monday = futureDay();
        while (monday.getDayOfWeek().getValue() != 1) monday = monday.plusDays(1);
        ExcludedDays holiday = new ExcludedDays(); holiday.setDate(monday.plusDays(2)); holiday.setDescription("Company Holiday");
        excludedDaysRepo.saveAndFlush(holiday);
        CourseApplication period = form(); period.setCourseStartDate(monday); period.setCourseEndDate(monday.plusDays(7));
        assertEquals(5d, staffService.saveApplication(null, period, staff).getTrainingDays());
        staffService.deleteApplication(period.getCourseId(), staff);
        CourseApplication morning = form(); morning.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING);
        morning.setCourseStartDate(monday); morning.setCourseEndDate(monday); morning.setHalfDayPeriod("AM");
        assertEquals(0.5, staffService.saveApplication(null, morning, staff).getTrainingDays());
        assertEquals(0, morning.getCourseFee().signum());
        CourseApplication afternoon = form(); afternoon.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING);
        afternoon.setCourseStartDate(monday); afternoon.setCourseEndDate(monday); afternoon.setHalfDayPeriod("PM");
        assertEquals(0.5, staffService.saveApplication(null, afternoon, staff).getTrainingDays());
        CourseApplication fullDay = form(); fullDay.setCourseStartDate(monday); fullDay.setCourseEndDate(monday);
        assertThrows(IllegalArgumentException.class, () -> staffService.saveApplication(null, fullDay, staff));
        staffService.deleteApplication(morning.getCourseId(), staff);
        staffService.deleteApplication(afternoon.getCourseId(), staff);
        CourseApplication training = form();
        training.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING);
        training.setCourseStartDate(monday);
        training.setCourseEndDate(monday.plusDays(1));
        training.setHalfDayPeriod("AM");
        assertThrows(IllegalArgumentException.class, () -> staffService.saveApplication(null, training, staff));
        training.setHalfDayPeriod(null);
        assertEquals(2d, staffService.saveApplication(null, training, staff).getTrainingDays());
        CourseApplication overlapping = form();
        overlapping.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING);
        overlapping.setCourseStartDate(monday);
        overlapping.setCourseEndDate(monday);
        overlapping.setHalfDayPeriod("PM");
        assertThrows(IllegalArgumentException.class, () -> staffService.saveApplication(null, overlapping, staff));
    }

    // Fee claims: Check course completion, documents and the manager decision.
    @Test
    void feeClaimRequiresCompletedCourseAndDocumentsAndShowsManagerReason() throws Exception {
        CourseApplication course = saved(ApplicationStatus.COMPLETED, LocalDate.now().minusDays(2));
        course.setCourseTitle("AWS Cloud Practitioner Essentials");
        course.setTrainingProvider("AWS");
        byte[] pdf = new byte[100000];
        System.arraycopy("%PDF-1.4".getBytes(), 0, pdf, 0, 8);
        MockMultipartFile receipt = new MockMultipartFile("receipt", "receipt.jpg", "image/jpeg", pdf);
        MockMultipartFile certificate = new MockMultipartFile("certificate", "certificate.pdf", "application/pdf", pdf);
        assertThrows(IllegalArgumentException.class, () -> staffService.submitClaim(course.getCourseId(), false, receipt, certificate, staff));
        mvc.perform(multipart("/staff/fee").file(receipt).file(certificate).session(session)
                .param("courseId", course.getCourseId().toString()).param("paidPersonally", "true"))
                .andExpect(redirectedUrl("/staff/fee"));
        claimRepo.flush();
        CourseFeeApplication claim = staffService.getClaims(staff).getFirst();
        assertEquals(ApplicationStatus.APPLIED, claim.getApplicationStatus());
        assertEquals("receipt.jpg", claim.getReceiptFileName());
        assertEquals("image/jpeg", claim.getReceiptContentType());
        assertEquals("certificate.pdf", claim.getCertificateFileName());
        assertThrows(IllegalArgumentException.class, () -> staffService.submitClaim(course.getCourseId(), true, receipt, certificate, staff));
        claimService.approveFeeApplication(claim.getApplicationId(), "Receipt verified");
        mvc.perform(get("/staff/claims/" + claim.getApplicationId()).session(session))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Receipt verified")));
        mvc.perform(get("/staff/claims/" + claim.getApplicationId() + "/receipt").session(session))
                .andExpect(status().isOk()).andExpect(content().bytes(pdf))
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("receipt.jpg")));
        mvc.perform(get("/staff/fee").session(session)).andExpect(status().isOk());
    }

    // Pagination: Check the selected page and number of results.
    @Test
    void paginationRenders() throws Exception {
        for (int i = 0; i < 31; i++) saved(ApplicationStatus.DELETED, LocalDate.now());
        mvc.perform(get("/staff/personal").session(session).param("page", "1").param("size", "20"))
                .andExpect(status().isOk()).andExpect(model().attribute("lastPage", 1))
                .andExpect(model().attribute("applications", org.hamcrest.Matchers.hasSize(11)));
        assertEquals(31, staffService.getCourseHistory(staff, LocalDate.now().getYear()).size());
    }

    // Allowance and claims: Check completed course totals and invalid claim input.
    @Test
    void completedCoursesConsumeAllowanceAndClaimValidationRejectsInvalidInput() {
        saved(ApplicationStatus.COMPLETED, LocalDate.now().minusDays(2));
        allocate(staff, 1, 2000);
        assertThrows(IllegalArgumentException.class, () -> staffService.saveApplication(null, form(), staff));
        allocate(staff, 10, 500);
        assertThrows(IllegalArgumentException.class, () -> staffService.saveApplication(null, form(), staff));
        CourseApplication pending = saved(ApplicationStatus.APPLIED, futureDay());
        MockMultipartFile bad = new MockMultipartFile("receipt", "empty.pdf", "application/pdf", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> staffService.submitClaim(pending.getCourseId(), true, bad, bad, staff));
        CourseApplication completed = staffService.getCourseHistory(staff, LocalDate.now().getYear()).getFirst();
        assertThrows(IllegalArgumentException.class, () -> staffService.submitClaim(completed.getCourseId(), true, bad, bad, staff));
        assertTrue(staffService.getClaims(staff).isEmpty());
    }

    // Course fee: Allow an external course with no fee.
    @Test
    void externalCourseCanHaveZeroFee() {
        CourseApplication course = form();
        course.setCourseFee(new java.math.BigDecimal("0"));
        assertEquals(0, staffService.saveApplication(null, course, staff).getCourseFee().signum());
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
        Staff loaded = staffService.getStaff(staffId);
        assertEquals(managerId, loaded.getManager().getUserId());
        assertInstanceOf(Manager.class, loaded.getManager());
        assertEquals(2, staffService.getStaffByManager(managerId).size());
        assertTrue(staffService.getStaffByManager(-1).isEmpty());
        assertNull(staffService.getStaff(managerId).getManager());
        session.setAttribute("user", loaded);
        mvc.perform(get("/staff/home").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Michael")));
    }

    // Staff workflow: Check status rules and personal lists.
    @Test
    void staffStatusRulesAndPersonalListsArePreserved() throws Exception {
        for (ApplicationStatus state : List.of(ApplicationStatus.REJECTED, ApplicationStatus.CANCELLED,
                ApplicationStatus.DELETED, ApplicationStatus.COMPLETED)) {
            CourseApplication course = saved(state, LocalDate.now().minusDays(3));
            assertThrows(IllegalStateException.class, () -> staffService.saveApplication(course.getCourseId(), form(), staff));
            assertThrows(IllegalStateException.class, () -> staffService.deleteApplication(course.getCourseId(), staff));
            assertThrows(IllegalStateException.class, () -> staffService.cancelApplication(course.getCourseId(), staff));
            mvc.perform(get("/staff/applications/" + course.getCourseId()).session(session))
                    .andExpect(status().isOk());
        }
        Staff second = new Staff();
        second.setUserName("martin"); second.setPassword("test-password");
        second.setName("Martin"); second.setEmail("martin@example.test"); second.setRole(Roles.STAFF);
        second.setStaffId("S005");
        staffRepo.saveAndFlush(second);
        CourseApplication otherCourse = form(); otherCourse.setApplicant(second);
        applicationRepo.saveAndFlush(otherCourse);
        for (CourseApplication row : staffService.getCourseHistory(staff, LocalDate.now().getYear())) {
            assertEquals(staff.getUserId(), row.getApplicant().getUserId());
        }
        mvc.perform(get("/staff/applications/" + otherCourse.getCourseId()).session(session))
                .andExpect(redirectedUrl("/staff/home")).andExpect(flash().attributeExists("error"));
        mvc.perform(get("/staff/applications/" + otherCourse.getCourseId() + "/edit").session(session))
                .andExpect(redirectedUrl("/staff/home")).andExpect(flash().attributeExists("error"));
        CourseFeeApplication otherClaim = new CourseFeeApplication();
        otherClaim.setApplicant(second);
        otherClaim.setApplicationStatus(ApplicationStatus.APPLIED);
        otherClaim.setReceipt(new byte[] {1});
        otherClaim.setCertificate(new byte[] {1});
        otherClaim.setCourseApplication(otherCourse);
        otherClaim = claimRepo.saveAndFlush(otherClaim);
        mvc.perform(get("/staff/claims/" + otherClaim.getApplicationId()).session(session))
                .andExpect(status().isOk());
        for (String document : List.of("receipt", "certificate")) {
            mvc.perform(get("/staff/claims/" + otherClaim.getApplicationId() + "/" + document).session(session))
                    .andExpect(status().isOk());
            mvc.perform(get("/staff/claims/" + otherClaim.getApplicationId() + "/" + document))
                    .andExpect(status().isUnauthorized());
        }
    }
    // Each fixture owns one allowance row for this year; other years are independent.
    private void allocate(User employee, double days, double budget) {
        int year = LocalDate.now().getYear();
        TrainingEntitlement allowance = entitlements.findByStaff_UserIdAndYear(employee.getUserId(), year)
                .orElse(new TrainingEntitlement(year));
        allowance.setStaff(employee); allowance.setDayLimit(days);
        allowance.setBudget(java.math.BigDecimal.valueOf(budget));
        entitlements.saveAndFlush(allowance);
    }
}

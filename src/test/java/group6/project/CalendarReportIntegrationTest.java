package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import group6.project.model.*;
import group6.project.model.form.HolidayForm;
import group6.project.repo.*;
import group6.project.service.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;

@SpringBootTest
@Transactional
class CalendarReportIntegrationTest {
    @Autowired UserRepo users;
    @Autowired CourseApplicationRepo applications;
    @Autowired CourseFeeApplicationRepo claims;
    @Autowired CourseBatchRepo batches;
    @Autowired CourseDetailRepo offers;
    @Autowired CourseCategoryRepository categories;
    @Autowired ExcludedDaysRepo holidays;
    @Autowired ExcludedDaysService holidayService;
    @Autowired TrainingCalendarService calendar;
    @Autowired TrainingReportService reports;
    @Autowired TrainingEntitlementService allowances;
    @Autowired WebApplicationContext context;
    Staff sam, pat;
    Manager bob, peer;
    Admin admin;
    MockMvc mvc;

    // Real roles and reporting assignments exercise both page scope and persisted totals.
    @BeforeEach
    void prepare() {
        bob = account(new Manager(), "calendar_bob");
        peer = account(new Manager(), "calendar_peer");
        admin = account(new Admin(), "calendar_admin");
        sam = new Staff();
        sam.setManager(bob);
        sam = account(sam, "calendar_sam");
        pat = new Staff();
        pat.setManager(peer);
        pat = account(pat, "calendar_pat");
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // Calendar derives approved working-day attendance, with category filters and no private
    // application fields.
    @Test
    void allRolesSeeApprovedAttendanceWithoutPrivateDetails() throws Exception {
        LocalDate start =
                LocalDate.now().plusDays(15).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        var holiday = new ExcludedDays();
        holiday.setDate(start.plusDays(2));
        holiday.setDescription("Calendar fixture holiday");
        holidays.saveAndFlush(holiday);
        var course =
                course(
                        sam,
                        start,
                        ApplicationStatus.APPROVED,
                        CourseCategoryType.EXTERNAL_COURSE,
                        "100");
        course.setCourseEndDate(start.plusDays(4));
        course.setTrainingDays(4d);
        applications.saveAndFlush(course);
        var internal =
                course(
                        pat,
                        start,
                        ApplicationStatus.APPROVED,
                        CourseCategoryType.INTERNAL_TRAINING,
                        "0");
        internal.setHalfDayPeriod("AM");
        internal.setTrainingDays(.5);
        applications.saveAndFlush(internal);
        course(
                sam,
                start.plusDays(7),
                ApplicationStatus.APPLIED,
                CourseCategoryType.EXTERNAL_COURSE,
                "50");
        var weeks = calendar.month(YearMonth.from(start), null);
        assertTrue(weeks.stream().allMatch(w -> w.size() == 7));
        var first =
                weeks.stream()
                        .flatMap(java.util.List::stream)
                        .filter(d -> d.date().equals(start))
                        .findFirst()
                        .orElseThrow();
        assertEquals(2, first.attendance().size());
        assertTrue(
                weeks.stream()
                        .flatMap(java.util.List::stream)
                        .filter(d -> d.weekend() || d.holiday() != null)
                        .allMatch(d -> d.attendance().isEmpty()));
        var filtered = calendar.month(YearMonth.from(start), CourseCategoryType.INTERNAL_TRAINING);
        assertEquals(
                1,
                filtered.stream()
                        .flatMap(java.util.List::stream)
                        .flatMap(d -> d.attendance().stream())
                        .count());
        for (User user : java.util.List.of(sam, bob, admin))
            mvc.perform(
                            get("/training/calendar")
                                    .session(session(user))
                                    .param("month", YearMonth.from(start).toString()))
                    .andExpect(status().isOk())
                    .andExpect(
                            content().string(org.hamcrest.Matchers.containsString("calendar_pat")))
                    .andExpect(
                            content()
                                    .string(
                                            org.hamcrest.Matchers.not(
                                                    org.hamcrest.Matchers.containsString(
                                                            "PRIVATE JUSTIFICATION"))));
        mvc.perform(get("/training/calendar")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/training/calendar").session(session(sam)).param("month", "invalid"))
                .andExpect(status().isBadRequest());
    }

    // Period course totals are separate from full-year reservations and actual claim payments.
    @Test
    void reportsUseCorrectEmployeePeriodAndPaymentScope() throws Exception {
        int year = LocalDate.now().getYear() - 1;
        LocalDate from = LocalDate.of(year, 3, 1), to = from.withDayOfMonth(31);
        allowances.saveLimits(sam.getUserId(), year, 10, new BigDecimal("2000"));
        course(
                sam,
                from.plusDays(2),
                ApplicationStatus.APPROVED,
                CourseCategoryType.EXTERNAL_COURSE,
                "100");
        var completed =
                course(
                        sam,
                        from.plusDays(9),
                        ApplicationStatus.COMPLETED,
                        CourseCategoryType.PROFESSIONAL_CERTIFICATION,
                        "200");
        completed.setCourseTitle("=HYPERLINK(\"https://example.invalid\")");
        applications.saveAndFlush(completed);
        course(
                sam,
                to.plusDays(2),
                ApplicationStatus.APPLIED,
                CourseCategoryType.EXTERNAL_COURSE,
                "50");
        course(
                pat,
                from.plusDays(2),
                ApplicationStatus.APPLIED,
                CourseCategoryType.EXTERNAL_COURSE,
                "999");
        var claim = new CourseFeeApplication();
        claim.setApplicant(sam);
        claim.setCourseApplication(completed);
        claim.setApprovalManager(bob);
        claim.setReviewer(bob);
        claim.setApplicationStatus(ApplicationStatus.APPROVED);
        claim.setAmount(new BigDecimal("200"));
        claims.saveAndFlush(claim);
        var report = reports.report(bob, from, to, null, null, false);
        assertEquals(2, report.rows().size());
        assertEquals(0, new BigDecimal("300").compareTo(report.committedFees()));
        assertEquals(0, new BigDecimal("200").compareTo(report.approvedClaims()));
        assertEquals(0, report.reimbursed().signum());
        assertEquals(
                0,
                new BigDecimal("350").compareTo(report.annual().getFirst().summary().usedBudget()));
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> reports.report(bob, from, to, null, pat.getUserId(), false));
        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> reports.report(sam, from, to, null, null, false));
        String csv = new String(reports.csv(report), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.contains("'="));
        assertTrue(csv.contains("\"\"https://example.invalid\"\""));
        mvc.perform(
                        get("/manager/reports")
                                .session(session(bob))
                                .param("from", from.toString())
                                .param("to", to.toString()))
                .andExpect(status().isOk());
        mvc.perform(
                        get("/admin/reports")
                                .session(session(admin))
                                .param("from", from.toString())
                                .param("to", to.toString()))
                .andExpect(status().isOk());
        mvc.perform(
                        get("/manager/reports.csv")
                                .session(session(bob))
                                .param("from", from.toString())
                                .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(reports.csv(report)));
        assertEquals(
                1,
                reports.report(bob, from, to, CourseCategoryType.EXTERNAL_COURSE, null, false)
                        .rows()
                        .size());
        assertThrows(
                IllegalArgumentException.class,
                () -> reports.report(admin, from, to.plusYears(1), null, null, false));
    }

    // Attendance includes a course already in progress and excludes pending requests.
    @Test
    void attendanceIncludesCoursesStartingBeforeTheFilter() throws Exception {
        LocalDate start =
                LocalDate.of(LocalDate.now().getYear() - 1, 3, 3)
                        .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        var approved =
                course(
                        sam,
                        start,
                        ApplicationStatus.APPROVED,
                        CourseCategoryType.EXTERNAL_COURSE,
                        "100");
        approved.setCourseEndDate(start.plusDays(4));
        approved.setTrainingDays(5d);
        applications.saveAndFlush(approved);
        course(
                sam,
                start.plusDays(2),
                ApplicationStatus.APPLIED,
                CourseCategoryType.EXTERNAL_COURSE,
                "50");
        LocalDate from = start.plusDays(2), to = start.plusDays(3);
        assertEquals(2, reports.report(bob, from, to, null, null, false).rows().size());
        var attendance = reports.report(bob, from, to, null, null, true);
        assertEquals(1, attendance.rows().size());
        assertEquals(start, attendance.rows().getFirst().start());
        assertEquals(5d, attendance.rows().getFirst().days());
        mvc.perform(
                        get("/manager/reports")
                                .session(session(bob))
                                .param("from", from.toString())
                                .param("to", to.toString())
                                .param("attendanceOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("rows", org.hamcrest.Matchers.hasSize(1)));
        mvc.perform(
                        get("/manager/reports.csv")
                                .session(session(bob))
                                .param("from", from.toString())
                                .param("to", to.toString())
                                .param("attendanceOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(reports.csv(attendance)));
    }

    // Imported batch claims contribute to payments without inventing attendance or course
    // reservations.
    @Test
    void reportsIncludeBatchOnlyLegacyClaimsInTheCorrectScope() throws Exception {
        LocalDate day = LocalDate.of(LocalDate.now().getYear() - 1, 3, 3);
        var offer = new CourseDetail();
        offer.setTitle("Legacy paid training");
        offer.setCourseFee(new BigDecimal("200"));
        offer.setCourseCategory(
                categories.findAll().stream()
                        .filter(c -> c.getKind() == CourseCategoryType.EXTERNAL_COURSE)
                        .findFirst()
                        .orElseThrow());
        offer = offers.saveAndFlush(offer);
        var batch = new CourseBatch();
        batch.setCourseDetail(offer);
        batch.setCourseStartDate(day);
        batch.setCourseEndDate(day);
        batch = batches.saveAndFlush(batch);
        var claim = new CourseFeeApplication();
        claim.setApplicant(sam);
        claim.setCourseBatch(batch);
        claim.setAmount(new BigDecimal("200"));
        claim.setApplicationStatus(ApplicationStatus.APPROVED);
        claim.setReimbursedAt(LocalDateTime.now());
        claims.saveAndFlush(claim);
        var report = reports.report(bob, day, day, null, null, false);
        assertEquals(1, report.rows().size());
        assertTrue(report.rows().getFirst().legacy());
        assertEquals(0, new BigDecimal("200").compareTo(report.approvedClaims()));
        assertEquals(0, new BigDecimal("200").compareTo(report.reimbursed()));
        assertEquals(0, report.committedFees().signum());
        assertTrue(reports.report(peer, day, day, null, null, false).rows().isEmpty());
        assertTrue(
                reports.report(bob, day.plusDays(1), day.plusDays(1), null, null, false)
                        .rows()
                        .isEmpty());
        assertTrue(
                reports.report(bob, day, day, CourseCategoryType.INTERNAL_TRAINING, null, false)
                        .rows()
                        .isEmpty());
        assertTrue(reports.report(bob, day, day, null, null, true).rows().isEmpty());
        mvc.perform(
                        get("/admin/reports")
                                .session(session(admin))
                                .param("from", day.toString())
                                .param("to", day.toString()))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Legacy paid training")));
        assertTrue(
                new String(reports.csv(report), java.nio.charset.StandardCharsets.UTF_8)
                        .contains("Legacy claim"));
    }

    // Admin holiday changes cannot invalidate active periods, but a label correction remains safe.
    @Test
    void holidaysProtectActiveSchedulesAndUsePostDeletion() throws Exception {
        LocalDate day =
                LocalDate.now().plusDays(15).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
        var form = new HolidayForm();
        form.setDate(day);
        form.setDescription("Test holiday");
        holidayService.save(null, form);
        var saved =
                holidays.findAll().stream()
                        .filter(h -> h.getDate().equals(form.getDate()))
                        .findFirst()
                        .orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> holidayService.save(null, form));
        var course =
                course(
                        sam,
                        day.minusDays(1),
                        ApplicationStatus.APPROVED,
                        CourseCategoryType.EXTERNAL_COURSE,
                        "100");
        course.setCourseEndDate(day.plusDays(1));
        course.setTrainingDays(2d);
        applications.saveAndFlush(course);
        assertThrows(
                IllegalArgumentException.class,
                () -> holidayService.delete(saved.getId(), saved.getVersion()));
        var correction = holidayService.form(saved.getId());
        correction.setDescription("Corrected holiday");
        holidayService.save(saved.getId(), correction);
        mvc.perform(get("/admin/excludedDays").session(session(admin)))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(org.hamcrest.Matchers.containsString("Corrected holiday")));
        mvc.perform(get("/admin/excludedDays/edit/" + saved.getId()).session(session(admin)))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/deleteExcludedDays/" + saved.getId()).session(session(admin)))
                .andExpect(status().is3xxRedirection());
        assertTrue(holidays.existsById(saved.getId()));
        course.setStatus(ApplicationStatus.COMPLETED);
        applications.saveAndFlush(course);
        holidayService.delete(saved.getId(), saved.getVersion());
        assertFalse(holidays.existsById(saved.getId()));
    }

    // Reports paginate HTML while CSV includes every filtered row.
    @Test
    void csvDoesNotStopAtThePageBoundary() throws Exception {
        int year = LocalDate.now().getYear() - 1;
        LocalDate from = LocalDate.of(year, 1, 1), to = LocalDate.of(year, 12, 31);
        for (int i = 0; i < 31; i++)
            course(
                    sam,
                    from.plusDays(10 + i),
                    ApplicationStatus.DELETED,
                    CourseCategoryType.EXTERNAL_COURSE,
                    "100");
        mvc.perform(
                        get("/manager/reports")
                                .session(session(bob))
                                .param("from", from.toString())
                                .param("to", to.toString())
                                .param("page", "1")
                                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("rows", org.hamcrest.Matchers.hasSize(11)));
        assertEquals(
                32,
                new String(
                                reports.csv(reports.report(bob, from, to, null, null, false)),
                                java.nio.charset.StandardCharsets.UTF_8)
                        .lines()
                        .count());
    }

    // Fixtures represent stored history; read-only reports must not apply new-submission date
    // rules.
    private CourseApplication course(
            User employee,
            LocalDate date,
            ApplicationStatus status,
            CourseCategoryType category,
            String fee) {
        var course = new CourseApplication();
        course.setApplicant(employee);
        course.setApprovalManager(employee.getManager());
        course.setCourseTitle("Training fixture");
        course.setCourseStartDate(date);
        course.setCourseEndDate(date);
        course.setCourseCategory(category);
        course.setTrainingProvider("ISS");
        course.setCourseFee(new BigDecimal(fee));
        course.setTrainingDays(1d);
        course.setStatus(status);
        course.setJustification("PRIVATE JUSTIFICATION");
        course.setExperienceComments(
                status == ApplicationStatus.COMPLETED ? "Useful training" : null);
        return applications.saveAndFlush(course);
    }

    // Create a distinct persisted account without depending on development seeds.
    private <T extends User> T account(T user, String name) {
        user.setUserName(name);
        user.setStaffId(name);
        user.setName(name);
        user.setPassword("test");
        return users.saveAndFlush(user);
    }

    // Create the shared authenticated session contract used by MVC pages.
    private MockHttpSession session(User user) {
        var session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }
}

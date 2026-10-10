package group6.project;

import static group6.project.TestRequests.post;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import group6.project.form.*;
import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApplicationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepo users;
    @Autowired CourseApplicationRepo applications;
    @Autowired CourseApplicationService service;
    @Autowired ManagerService managers;
    @Autowired TrainingEntitlementService allowances;
    @Autowired CourseDetailService courses;
    @Autowired CourseBatchService batches;
    @Autowired CourseProviderRepo providers;
    @Autowired CourseDetailRepo courseRepo;
    @Autowired CourseBatchRepo batchRepo;
    @Autowired ApprovalRoutingService routing;
    @Autowired ExcludedDaysService holidays;
    private Manager manager;
    private Manager peer;
    private Staff staff;
    private CourseDetail offer;
    private LocalDate monday;

    @BeforeEach
    void setup() {
        monday =
                LocalDate.now()
                        .plusYears(1)
                        .withDayOfYear(1)
                        .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        manager = new Manager();
        manager.setName("Manager");
        manager.setUserName("workflow-manager");
        manager.setPassword("demo123");
        manager = users.saveAndFlush(manager);
        peer = new Manager();
        peer.setName("Peer");
        peer.setUserName("workflow-peer");
        peer.setPassword("demo123");
        peer = users.saveAndFlush(peer);
        staff = new Staff();
        staff.setName("Employee");
        staff.setUserName("workflow-staff");
        staff.setPassword("demo123");
        staff.setManager(manager);
        staff = users.saveAndFlush(staff);
        allowances.saveLimits(staff.getUserId(), monday.getYear(), 10, new BigDecimal("2000"));
        allowances.saveLimits(manager.getUserId(), monday.getYear(), 10, new BigDecimal("2000"));
        CourseProvider provider = new CourseProvider();
        provider.setName("Workflow Academy");
        provider = providers.saveAndFlush(provider);
        CourseForm form = new CourseForm();
        form.setTitle("Catalogue Java");
        form.setCourseFee(new BigDecimal("400"));
        form.setCategoryId(2);
        form.setProviderId(provider.getProviderId());
        form.setCourseDescription("Practice Java together.");
        form.setCustomDatesAllowed(true);
        form.setActive(true);
        offer = courses.save(null, form);
        courseRepo.flush();
    }

    @Test
    void submitApproveAndReadResultUsesOneWorkflow() throws Exception {
        CourseApplication saved = service.createOther(other(), staff);
        assertEquals(manager.getUserId(), saved.getApprovalManager().getUserId());
        assertEquals(
                saved.getCourseId(),
                managers.getPendingApplicationGroups(manager.getUserId())
                        .getFirst()
                        .applications()
                        .getFirst()
                        .getCourseId());
        assertEquals(
                new BigDecimal("100"),
                allowances.summary(staff, monday.getYear(), null).reservedBudget());
        mvc.perform(
                        get("/manager/applications/" + saved.getCourseId())
                                .sessionAttr("user", manager))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Other approved absences")));
        mvc.perform(
                        post("/manager/applications/" + saved.getCourseId() + "/decision")
                                .sessionAttr("user", manager)
                                .param("version", version(saved).toString())
                                .param("approved", "true")
                                .param("reason", "Useful for our project"))
                .andExpect(redirectedUrl("/manager/applications/" + saved.getCourseId()));
        assertEquals(ApplicationStatus.APPROVED, saved.getStatus());
        assertEquals(manager.getUserId(), saved.getReviewer().getUserId());
        assertEquals(
                0, allowances.summary(staff, monday.getYear(), null).reservedBudget().signum());
        assertEquals(
                0,
                new BigDecimal("100")
                        .compareTo(allowances.summary(staff, monday.getYear(), null).usedBudget()));
        mvc.perform(get("/staff/applications/" + saved.getCourseId()).sessionAttr("user", staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Useful for our project")));
    }

    @Test
    void invalidDecisionKeepsReasonAndDoesNotWrite() throws Exception {
        var saved = service.createOther(other(), staff);
        mvc.perform(
                        post("/manager/applications/" + saved.getCourseId() + "/decision")
                                .sessionAttr("user", manager)
                                .param("version", version(saved).toString())
                                .param("approved", "false")
                                .param("reason", " "))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("decision", "reason"));
        assertEquals(ApplicationStatus.APPLIED, saved.getStatus());
    }

    @Test
    void stalePageCannotOverwriteAnEdit() {
        var saved = service.createOther(other(), staff);
        Long old = version(saved);
        var edit = other();
        edit.setVersion(old);
        edit.setCourseTitle("Updated title");
        service.updateOther(saved.getCourseId(), edit, staff);
        applications.flush();
        assertEquals(
                409,
                assertThrows(
                                ResponseStatusException.class,
                                () ->
                                        service.decide(
                                                saved.getCourseId(), decision(old, true), manager))
                        .getStatusCode()
                        .value());
        assertEquals(ApplicationStatus.UPDATED, saved.getStatus());
    }

    @Test
    void decidedApplicationCannotBeDecidedTwice() {
        var saved = service.createOther(other(), staff);
        service.decide(saved.getCourseId(), decision(version(saved), false), manager);
        assertEquals(
                400,
                assertThrows(
                                ResponseStatusException.class,
                                () ->
                                        service.decide(
                                                saved.getCourseId(),
                                                decision(version(saved), true),
                                                manager))
                        .getStatusCode()
                        .value());
        assertEquals(ApplicationStatus.REJECTED, saved.getStatus());
    }

    @Test
    void expiredRequestCanBeRejectedButNotApproved() {
        var saved = service.createOther(other(), staff);
        saved.setCourseStartDate(LocalDate.now().minusDays(3));
        saved.setCourseEndDate(LocalDate.now().minusDays(2));
        var form = decision(version(saved), true);
        assertEquals(
                400,
                assertThrows(
                                ResponseStatusException.class,
                                () -> service.decide(saved.getCourseId(), form, manager))
                        .getStatusCode()
                        .value());
        form.setApproved(false);
        service.decide(saved.getCourseId(), form, manager);
        assertEquals(ApplicationStatus.REJECTED, saved.getStatus());
    }

    @Test
    void ownerAndAssignedManagerChecksRejectGuessedIds() throws Exception {
        var saved = service.createOther(other(), staff);
        assertEquals(
                403,
                assertThrows(
                                ResponseStatusException.class,
                                () ->
                                        service.decide(
                                                saved.getCourseId(),
                                                decision(version(saved), true),
                                                peer))
                        .getStatusCode()
                        .value());
        mvc.perform(get("/staff/applications/" + saved.getCourseId()).sessionAttr("user", peer))
                .andExpect(status().isNotFound());
        mvc.perform(get("/manager/applications/" + saved.getCourseId()).sessionAttr("user", peer))
                .andExpect(status().isNotFound());
        mvc.perform(get("/manager/approvals").sessionAttr("user", staff))
                .andExpect(status().isForbidden());
    }

    @Test
    void rootManagerSelectsPeerWithoutSelfApproval() throws Exception {
        assertTrue(
                routing.choices(manager).stream()
                        .noneMatch(user -> user.getUserId().equals(manager.getUserId())));
        var form = other();
        form.setReviewerId(peer.getUserId());
        var saved = service.createOther(form, manager);
        assertEquals(peer.getUserId(), saved.getApprovalManager().getUserId());
        mvc.perform(get("/manager/applications/" + saved.getCourseId()).sessionAttr("user", peer))
                .andExpect(status().isOk());
        assertEquals(
                403,
                assertThrows(
                                ResponseStatusException.class,
                                () ->
                                        service.decide(
                                                saved.getCourseId(),
                                                decision(version(saved), true),
                                                manager))
                        .getStatusCode()
                        .value());
        form.setReviewerId(manager.getUserId());
        assertThrows(ResponseStatusException.class, () -> service.createOther(form, manager));
    }

    @Test
    void staffCannotReplaceTheReportingManager() {
        var form = other();
        form.setReviewerId(peer.getUserId());
        assertThrows(ResponseStatusException.class, () -> service.createOther(form, staff));
        assertTrue(routing.choices(staff).isEmpty());
    }

    @Test
    void catalogueSubmissionIgnoresSpoofedPriceAndIdentity() throws Exception {
        mvc.perform(
                        get("/staff/courses/" + offer.getCourseId() + "/apply")
                                .sessionAttr("user", staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Catalogue Java")));
        mvc.perform(
                        post("/staff/courses/" + offer.getCourseId() + "/apply")
                                .sessionAttr("user", staff)
                                .param("courseVersion", offer.getVersion().toString())
                                .param("courseStartDate", monday.toString())
                                .param("courseEndDate", monday.toString())
                                .param("justification", "Learn useful Java skills")
                                .param("courseFee", "1")
                                .param("courseTitle", "Forged title")
                                .param("applicant.userId", peer.getUserId().toString())
                                .param("status", "APPROVED"))
                .andExpect(status().is3xxRedirection());
        var saved = service.findForStaffAndYear(staff, monday.getYear()).getFirst();
        assertEquals("Catalogue Java", saved.getCourseTitle());
        assertEquals(0, new BigDecimal("400").compareTo(saved.getCourseFee()));
        assertEquals(staff.getUserId(), saved.getApplicant().getUserId());
        assertEquals(ApplicationStatus.APPLIED, saved.getStatus());
    }

    @Test
    void fixedScheduleOwnsDatesAndReservesCapacity() {
        var batch = batch(1);
        var form = catalogue();
        form.setBatchId(batch.getBatchId());
        form.setBatchVersion(batch.getVersion());
        form.setCourseStartDate(monday.plusDays(4));
        form.setCourseEndDate(monday.plusDays(4));
        var saved = service.createFromCatalogue(offer.getCourseId(), form, staff);
        assertEquals(monday, saved.getCourseStartDate());
        var second = catalogue();
        second.setReviewerId(peer.getUserId());
        second.setBatchId(batch.getBatchId());
        second.setBatchVersion(batch.getVersion());
        assertEquals(
                400,
                assertThrows(
                                ResponseStatusException.class,
                                () ->
                                        service.createFromCatalogue(
                                                offer.getCourseId(), second, manager))
                        .getStatusCode()
                        .value());
        service.delete(saved.getCourseId(), version(saved), staff);
        assertEquals(
                batch.getBatchId(),
                service.createFromCatalogue(offer.getCourseId(), second, manager)
                        .getCatalogueBatch()
                        .getBatchId());
    }

    @Test
    void catalogueEditsKeepOriginalPriceCategoryAndDates() throws Exception {
        var batch = batch(2);
        var form = catalogue();
        form.setBatchId(batch.getBatchId());
        form.setBatchVersion(batch.getVersion());
        var saved = service.createFromCatalogue(offer.getCourseId(), form, staff);
        offer.setCourseFee(new BigDecimal("900"));
        offer.setTitle("Changed title");
        offer.setActive(false);
        courseRepo.flush();
        form.setVersion(version(saved));
        form.setCourseStartDate(monday.plusDays(4));
        form.setCourseEndDate(monday.plusDays(4));
        form.setJustification("More detail");
        service.updateCatalogue(saved.getCourseId(), form, staff);
        assertEquals(0, new BigDecimal("400").compareTo(saved.getCourseFee()));
        assertEquals("Catalogue Java", saved.getCourseTitle());
        assertEquals(monday, saved.getCourseStartDate());
        mvc.perform(
                        get("/staff/applications/" + saved.getCourseId() + "/edit")
                                .sessionAttr("user", staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Catalogue Java")));
    }

    @Test
    void changedOfferOrScheduleRequiresReloading() {
        var batch = batch(2);
        var form = catalogue();
        form.setCourseVersion(-1L);
        assertEquals(
                409,
                assertThrows(
                                ResponseStatusException.class,
                                () -> service.createFromCatalogue(offer.getCourseId(), form, staff))
                        .getStatusCode()
                        .value());
        form.setCourseVersion(offer.getVersion());
        form.setBatchId(batch.getBatchId());
        form.setBatchVersion(-1L);
        assertEquals(
                409,
                assertThrows(
                                ResponseStatusException.class,
                                () -> service.createFromCatalogue(offer.getCourseId(), form, staff))
                        .getStatusCode()
                        .value());
    }

    @Test
    void badFormPreservesInputAndNeverSaves() throws Exception {
        long before = applications.count();
        mvc.perform(
                        post("/staff/apply/other")
                                .sessionAttr("user", staff)
                                .param("courseTitle", "Keep my title")
                                .param("courseFee", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Keep my title")))
                .andExpect(model().attributeHasFieldErrors("form", "courseFee"));
        assertEquals(before, applications.count());
        mvc.perform(get("/staff/apply").sessionAttr("user", staff))
                .andExpect(redirectedUrl("/staff/courses"));
    }

    @Test
    void pendingAllowanceCannotBeSpentAgain() {
        allowances.saveLimits(staff.getUserId(), monday.getYear(), 1, new BigDecimal("100"));
        var saved = service.createOther(other(), staff);
        var another = other();
        another.setCourseStartDate(monday.plusDays(1));
        another.setCourseEndDate(monday.plusDays(1));
        assertThrows(ResponseStatusException.class, () -> service.createOther(another, staff));
        service.decide(saved.getCourseId(), decision(version(saved), false), manager);
        assertEquals(ApplicationStatus.APPLIED, service.createOther(another, staff).getStatus());
    }

    @Test
    void historyAndDecisionSupportUseActualTeamAndCurrentYear() throws Exception {
        var selected = service.createOther(other(), staff);
        Staff colleague = new Staff();
        colleague.setName("Colleague");
        colleague.setUserName("workflow-colleague");
        colleague.setPassword("demo123");
        colleague.setManager(manager);
        colleague = users.saveAndFlush(colleague);
        CourseApplication absence = new CourseApplication();
        absence.setApplicant(colleague);
        absence.setApprovalManager(manager);
        absence.setCourseTitle("Other approved course");
        absence.setTrainingProvider("Provider");
        absence.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING);
        absence.setCourseStartDate(monday);
        absence.setCourseEndDate(monday);
        absence.setStatus(ApplicationStatus.APPROVED);
        absence.setTrainingDays(1d);
        applications.saveAndFlush(absence);
        assertEquals(List.of(absence), managers.overlaps(manager.getUserId(), selected));
        selected.setCourseStartDate(LocalDate.now());
        selected.setCourseEndDate(LocalDate.now());
        applications.flush();
        mvc.perform(
                        get("/manager/history")
                                .sessionAttr("user", manager)
                                .param("employeeId", staff.getUserId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("External Java")));
        mvc.perform(
                        get("/manager/history")
                                .sessionAttr("user", peer)
                                .param("employeeId", staff.getUserId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void holidayCannotChangeAnActiveApplicationDuration() {
        service.createOther(other(), staff);
        ExcludedDays holiday = new ExcludedDays();
        holiday.setDate(monday);
        holiday.setDescription("New holiday");
        assertEquals(
                400,
                assertThrows(ResponseStatusException.class, () -> holidays.addExcludedDay(holiday))
                        .getStatusCode()
                        .value());
    }

    private CourseApplicationForm other() {
        var form = new CourseApplicationForm();
        form.setCourseTitle("External Java");
        form.setTrainingProvider("Example Academy");
        form.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);
        form.setCourseFee(new BigDecimal("100"));
        form.setCourseStartDate(monday);
        form.setCourseEndDate(monday);
        form.setJustification("Improve our Java project");
        return form;
    }

    private CatalogueApplicationForm catalogue() {
        var form = new CatalogueApplicationForm();
        form.setCourseVersion(offer.getVersion());
        form.setCourseStartDate(monday);
        form.setCourseEndDate(monday);
        form.setJustification("Improve our Java project");
        return form;
    }

    private CourseBatch batch(int capacity) {
        CourseBatchForm form = new CourseBatchForm();
        form.setCourseId(offer.getCourseId());
        form.setStartDate(monday);
        form.setEndDate(monday);
        form.setCapacity(capacity);
        form.setActive(true);
        var saved = batches.save(null, form);
        batchRepo.flush();
        return saved;
    }

    private Long version(CourseApplication application) {
        applications.flush();
        return application.getVersion();
    }

    private DecisionForm decision(Long version, boolean approved) {
        var form = new DecisionForm();
        form.setVersion(version);
        form.setApproved(approved);
        form.setReason("Reviewed the course details");
        return form;
    }
}

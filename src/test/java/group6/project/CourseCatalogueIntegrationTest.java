package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import group6.project.model.*;
import group6.project.model.form.*;
import group6.project.repo.*;
import group6.project.service.*;

@SpringBootTest
@Transactional
class CourseCatalogueIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired StaffRepo employees;
    @Autowired CourseCategoryRepository categories;
    @Autowired CourseDetailRepo courses;
    @Autowired CourseBatchRepo batches;
    @Autowired CourseApplicationRepo applications;
    @Autowired TrainingEntitlementService allowances;
    @Autowired CourseCatalogueService catalogue;
    @Autowired CourseDetailService maintenance;
    @Autowired CourseBatchService schedules;
    @Autowired CourseApplicationService policy;
    @Autowired UserRepo users;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entities;
    Staff employee; CourseDetail course; CourseBatch batch; LocalDate day; MockMvc mvc;

    // Use actual migrations, persisted offers and MVC rendering, with rollback after each case.
    @BeforeEach
    void prepare() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        employee = new Staff(); employee.setUserName("catalogue_fixture"); employee.setName("Catalogue fixture");
        employee.setPassword("test"); employee.setStaffId("CAT"); employee.setRole(Roles.STAFF);
        Manager reportingManager = new Manager();
        reportingManager.setUserName("review_" + java.util.UUID.randomUUID()); reportingManager.setStaffId("REVIEW_" + java.util.UUID.randomUUID());
        reportingManager.setName("Review fixture"); reportingManager.setPassword("test"); reportingManager.setRole(Roles.MANAGER);
        reportingManager = employees.saveAndFlush(reportingManager);
        employee.setManager(reportingManager);
        employee = employees.saveAndFlush(employee);
        day = LocalDate.now().plusDays(7); while (day.getDayOfWeek().getValue() > 5) day = day.plusDays(1);
        allowances.saveLimits(employee.getUserId(), day.getYear(), 10, new BigDecimal("2000.00"));
        course = new CourseDetail(); course.setTitle("Catalogue Java"); course.setTrainingProvider("Training centre");
        course.setCourseFee(new BigDecimal("123.45")); course.setCourseDescription("Practical skills");
        course.setCourseCategory(categories.findAll().stream().filter(c -> c.getKind() == CourseCategoryType.EXTERNAL_COURSE).findFirst().orElseThrow());
        course = courses.saveAndFlush(course);
        batch = new CourseBatch(); batch.setCourseDetail(course); batch.setCourseStartDate(day); batch.setCourseEndDate(day); batch.setCapacity(2);
        batch = batches.saveAndFlush(batch);
    }

    // Employee fields cannot replace server-owned prices, categories, providers or scheduled dates.
    @Test
    void mvcUsesPublishedMetadataAndPreviewDoesNotReserve() throws Exception {
        MockHttpSession session = session(employee);
        mvc.perform(get("/staff/apply").session(session).param("q", "java"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Catalogue Java")));
        mvc.perform(get("/staff/courses/" + course.getCourseId()).session(session)).andExpect(status().isOk());
        mvc.perform(get("/staff/courses/" + course.getCourseId() + "/apply").session(session)).andExpect(status().isOk());
        long count = applications.count();
        mvc.perform(post("/staff/courses/" + course.getCourseId() + "/apply").session(session)
                .param("courseVersion", "0").param("scheduledBatch", key()).param("action", "preview"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("summary"));
        assertEquals(count, applications.count());
        mvc.perform(post("/staff/courses/" + course.getCourseId() + "/apply").session(session)
                .param("courseVersion", "0").param("scheduledBatch", key()).param("justification", "Improve my work")
                .param("courseFee", "0").param("courseCategory", "INTERNAL_TRAINING").param("trainingProvider", "Forged")
                .param("startDate", day.plusDays(20).toString())).andExpect(status().is3xxRedirection());
        CourseApplication saved = policy.findForStaffAndYear(employee, day.getYear()).getFirst();
        assertEquals(course.getTitle(), saved.getCourseTitle()); assertEquals(new BigDecimal("123.45"), saved.getCourseFee());
        assertEquals("Training centre", saved.getTrainingProvider()); assertEquals(CourseCategoryType.EXTERNAL_COURSE, saved.getCourseCategory());
        assertEquals(day, saved.getCourseStartDate()); assertEquals(batch.getBatchId(), saved.getCatalogueBatch().getBatchId());
    }

    // A stale offer or a session from another course must be reviewed again before submission.
    @Test
    void staleAndForeignChoicesAreRejected() {
        CatalogueApplicationForm form = form(); form.setCourseVersion(99L);
        assertThrows(IllegalArgumentException.class, () -> catalogue.submit(course.getCourseId(), form, employee));
        form.setCourseVersion(0L); form.setScheduledBatch(batch.getBatchId() + ":99");
        assertThrows(IllegalArgumentException.class, () -> catalogue.submit(course.getCourseId(), form, employee));
        CourseDetail other = new CourseDetail(); other.setTitle("Other"); other.setTrainingProvider("Other provider");
        other.setCourseCategory(course.getCourseCategory()); other = courses.saveAndFlush(other);
        CourseBatch foreign = new CourseBatch(); foreign.setCourseDetail(other); foreign.setCapacity(2);
        foreign.setCourseStartDate(day); foreign.setCourseEndDate(day); foreign = batches.saveAndFlush(foreign);
        form.setScheduledBatch(foreign.getBatchId() + ":0");
        assertThrows(IllegalArgumentException.class, () -> catalogue.submit(course.getCourseId(), form, employee));
        assertEquals(0, policy.findForStaffAndYear(employee, day.getYear()).size());
    }

    // Existing applications keep their agreed offer and dates after catalogue edits or archives.
    @Test
    void pendingEditsAndArchivingPreserveTheOriginalSnapshot() throws Exception {
        CourseApplication saved = catalogue.submit(course.getCourseId(), form(), employee);
        CatalogueCourseForm offer = maintenance.form(course); offer.setTitle("New name"); offer.setFee(new BigDecimal("500.00"));
        maintenance.save(course.getCourseId(), offer); maintenance.archive(course.getCourseId());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> catalogue.offer(course.getCourseId()));
        CatalogueApplicationForm edit = form(); edit.setApplicationVersion(saved.getVersion()); edit.setJustification("Updated reason");
        edit.setStartDate(day.plusDays(10)); edit.setEndDate(day.plusDays(10));
        CourseApplication updated = catalogue.edit(saved.getCourseId(), edit, employee);
        assertEquals("Catalogue Java", updated.getCourseTitle()); assertEquals(new BigDecimal("123.45"), updated.getCourseFee());
        assertEquals(day, updated.getCourseStartDate()); assertEquals(ApplicationStatus.UPDATED, updated.getStatus());
        mvc.perform(get("/staff/applications/" + saved.getCourseId() + "/edit-catalogue").session(session(employee))).andExpect(status().isOk());
        CourseApplication forged = new CourseApplication(); forged.setCourseFee(BigDecimal.ZERO);
        forged.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING); forged.setCourseTitle("Forged");
        forged.setJustification("Another valid reason");
        assertEquals(new BigDecimal("123.45"), policy.update(saved.getCourseId(), forged, employee).getCourseFee());
    }

    // Schedule edits cannot move booked dates or reduce capacity below reserved requests.
    @Test
    void scheduleChangesKeepBookedDatesAndCapacity() {
        catalogue.submit(course.getCourseId(), form(), employee);
        CatalogueBatchForm edit = schedules.form(batch); edit.setCapacity(1);
        assertEquals(1, schedules.save(batch.getBatchId(), edit).getCapacity());
        edit = schedules.form(batch); edit.setStartDate(day.plusDays(1)); edit.setEndDate(day.plusDays(1));
        CatalogueBatchForm moved = edit;
        assertThrows(IllegalArgumentException.class, () -> schedules.save(batch.getBatchId(), moved));
        schedules.archive(batch.getBatchId());
        assertThrows(IllegalArgumentException.class, () -> catalogue.submit(course.getCourseId(), form(), employee));
        assertEquals(1, policy.findForStaffAndYear(employee, day.getYear()).size());
    }

    // Custom dates are an explicit offer option, and only Internal Training permits a half day.
    @Test
    void customDatesUseTheSharedRules() {
        CatalogueApplicationForm request = form(); request.setScheduledBatch(""); request.setStartDate(day); request.setEndDate(day);
        assertThrows(IllegalArgumentException.class, () -> catalogue.submit(course.getCourseId(), request, employee));
        course.setCustomDatesAllowed(true); course.setCourseCategory(categories.findAll().stream()
                .filter(c -> c.getKind() == CourseCategoryType.INTERNAL_TRAINING).findFirst().orElseThrow());
        course.setCourseFee(BigDecimal.ZERO); courses.saveAndFlush(course); request.setCourseVersion(course.getVersion()); request.setHalfDayPeriod("AM");
        CourseApplication saved = catalogue.submit(course.getCourseId(), request, employee);
        assertEquals(0.5, saved.getTrainingDays()); assertEquals(0, saved.getCourseFee().signum()); assertNull(saved.getCatalogueBatch());
    }

    // Both new Admin forms render, and invalid input stays on the form rather than causing a 500.
    @Test
    void adminMaintenanceAndOtherCourseEntryRender() throws Exception {
        Admin admin = new Admin(); admin.setUserName("catalogue_admin"); admin.setName("Catalogue admin"); admin.setPassword("test");
        admin.setRole(Roles.ADMIN); admin = users.saveAndFlush(admin);
        for (String path : java.util.List.of("/admin/courses", "/admin/courses/new", "/admin/courses/edit/" + course.getCourseId(),
                "/admin/batches", "/admin/batches/new", "/admin/batches/edit/" + batch.getBatchId(), "/admin/categories")) {
            mvc.perform(get(path).session(session(admin))).andExpect(status().isOk());
        }
        mvc.perform(post("/admin/courses/save").session(session(admin)).param("title", ""))
                .andExpect(status().isOk()).andExpect(model().hasErrors());
        mvc.perform(get("/staff/apply/other").session(session(employee))).andExpect(status().isOk());
    }

    // A renamed label from the old Admin page still keeps its V2 business type on upgrade.
    @Test
    void migrationRestoresCategoryTypesUsingStableSeedIdentities() {
        entities.flush();
        jdbc.update("update course_category set category_name='Renamed internal label', kind=null where category_id=1");
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                    new org.springframework.core.io.ClassPathResource("db/migration/V8__restore_seeded_category_types.sql"))
                    .populate(connection);
            return null;
        });
        entities.clear();
        CourseCategory restored = categories.findById(1).orElseThrow();
        assertEquals("Renamed internal label", restored.getCategoryName());
        assertEquals(CourseCategoryType.INTERNAL_TRAINING, restored.getKind());
    }

    // Duplicate labels produce a useful flash message instead of a database constraint 500.
    @Test
    void duplicateCategoryRenameReturnsValidation() throws Exception {
        Admin admin = new Admin(); admin.setUserName("category_admin"); admin.setName("Category admin"); admin.setPassword("test");
        admin.setRole(Roles.ADMIN); admin = users.saveAndFlush(admin);
        String label = categories.findById(2).orElseThrow().getCategoryName();
        mvc.perform(post("/admin/categories/1/rename").session(session(admin)).param("label", label))
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("error", "Category name already exists."));
    }

    // A fully booked offer still shows its schedule, but cannot send staff to an unusable Apply form.
    @Test
    void fullScheduledOffersDoNotAdvertiseAnApplyAction() throws Exception {
        batch.setCapacity(1); batches.saveAndFlush(batch);
        catalogue.submit(course.getCourseId(), form(), employee);
        String html = mvc.perform(get("/staff/courses/" + course.getCourseId()).session(session(employee)))
                .andExpect(status().isOk()).andExpect(model().attribute("canApply", false))
                .andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("Apply for this course"));
        assertTrue(html.contains("No sessions have places available"));
    }

    // Every request carries the catalogue and schedule versions shown to this employee.
    private CatalogueApplicationForm form() {
        CatalogueApplicationForm form = new CatalogueApplicationForm(); form.setCourseVersion(course.getVersion());
        form.setScheduledBatch(key()); form.setJustification("Improve practical skills"); return form;
    }

    // The key identifies the selected schedule and the version of its dates/capacity.
    private String key() { return batch.getBatchId() + ":" + batch.getVersion(); }

    // Use the same session key as the real login controller.
    private MockHttpSession session(User user) { MockHttpSession session = new MockHttpSession(); session.setAttribute("user", user); return session; }
}

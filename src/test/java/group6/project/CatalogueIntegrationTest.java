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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogueIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepo users;
    @Autowired CourseProviderRepo providerRepo;
    @Autowired CourseDetailRepo courseRepo;
    @Autowired CourseBatchRepo batchRepo;
    @Autowired CourseApplicationRepo applications;
    @Autowired CourseDetailService courses;
    @Autowired CourseBatchService batches;
    @Autowired CourseCatalogueService catalogue;
    @Autowired CourseCategoryService categories;
    @Autowired CourseProviderService providers;
    private Admin admin;
    private Staff staff;
    private CourseProvider provider;
    private CourseDetail course;
    private final LocalDate monday =
            LocalDate.now()
                    .plusYears(1)
                    .withDayOfYear(1)
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    // Each test creates its own small catalogue; no development seed or SMTP is required.
    @BeforeEach
    void setup() {
        admin = new Admin();
        admin.setUserName("catalogue-admin");
        admin.setPassword("demo123");
        admin.setName("Catalogue Admin");
        admin = users.saveAndFlush(admin);
        staff = new Staff();
        staff.setUserName("catalogue-staff");
        staff.setPassword("demo123");
        staff.setName("Catalogue Staff");
        staff = users.saveAndFlush(staff);
        provider = new CourseProvider();
        provider.setName("Catalogue Demo Provider");
        provider = providerRepo.saveAndFlush(provider);
        course = courses.save(null, courseForm(2));
        courseRepo.flush();
    }

    @Test
    void allCatalogueAdminAndStaffPagesRenderWithSavedRows() throws Exception {
        var batch = batches.save(null, batchForm());
        batchRepo.flush();
        for (String path :
                new String[] {
                    "/admin/courses",
                    "/admin/courses/new",
                    "/admin/courses/" + course.getCourseId() + "/edit",
                    "/admin/batches",
                    "/admin/batches/new",
                    "/admin/batches/" + batch.getBatchId() + "/edit",
                    "/admin/providers",
                    "/admin/providers/new",
                    "/admin/providers/" + provider.getProviderId() + "/edit",
                    "/admin/categories",
                    "/admin/categories/new",
                    "/admin/categories/2/edit",
                    "/admin/schedule"
                }) {
            mvc.perform(get(path).sessionAttr("user", admin)).andExpect(status().isOk());
        }
        mvc.perform(get("/staff/courses").sessionAttr("user", staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Spring course")));
        mvc.perform(get("/staff/courses/" + course.getCourseId()).sessionAttr("user", staff))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("10 places left")));
    }

    @Test
    void formsRejectMissingValuesAndDoNotWrite() throws Exception {
        long before = courseRepo.count();
        for (String route : new String[] {"courses", "categories", "providers", "batches"}) {
            mvc.perform(post("/admin/" + route + "/new").sessionAttr("user", admin))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("alert-danger")));
        }
        assertEquals(before, courseRepo.count());
    }

    @Test
    void staffCannotMaintainCatalogueAndMissingCourseIs404() throws Exception {
        mvc.perform(post("/admin/courses/new").sessionAttr("user", staff))
                .andExpect(status().isForbidden());
        mvc.perform(get("/staff/courses/999999").sessionAttr("user", staff))
                .andExpect(status().isNotFound());
        mvc.perform(get("/admin/courses")).andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void internalFeesComeFromTheRuleAndSearchHonoursFilters() {
        CourseDetail internal = courses.save(null, courseForm(1));
        assertEquals(0, internal.getCourseFee().compareTo(BigDecimal.ZERO));
        assertEquals(2, catalogue.search("spring", null, provider.getProviderId()).size());
        assertEquals(
                1,
                catalogue.search("demo provider", CourseCategoryType.EXTERNAL_COURSE, null).size());
        assertTrue(catalogue.search("not found", null, null).isEmpty());
        provider.setActive(false);
        providerRepo.flush();
        assertTrue(catalogue.search("", null, null).isEmpty());
        assertEquals(
                404,
                assertThrows(
                                ResponseStatusException.class,
                                () -> catalogue.offer(course.getCourseId()))
                        .getStatusCode()
                        .value());
    }

    @Test
    void staleCourseFormCannotOverwriteAnotherEdit() {
        CourseForm first = courses.form(course.getCourseId());
        CourseForm stale = courses.form(course.getCourseId());
        first.setTitle("Updated course");
        courses.save(course.getCourseId(), first);
        courseRepo.flush();
        assertEquals(
                409,
                assertThrows(
                                ResponseStatusException.class,
                                () -> courses.save(course.getCourseId(), stale))
                        .getStatusCode()
                        .value());
        assertEquals("Updated course", courses.get(course.getCourseId()).getTitle());
    }

    @Test
    void categoryTypeCannotChangeUnderExistingCourses() {
        var form = categories.form(2);
        form.setKind(CourseCategoryType.INTERNAL_TRAINING);
        assertEquals(
                400,
                assertThrows(ResponseStatusException.class, () -> categories.save(2, form))
                        .getStatusCode()
                        .value());
        assertEquals(
                400,
                assertThrows(
                                ResponseStatusException.class,
                                () -> categories.delete(2, form.getVersion()))
                        .getStatusCode()
                        .value());
    }

    @Test
    void batchCalculatesWorkingDaysAndRejectsInvalidHalfDay() {
        var form = batchForm();
        form.setStartDate(monday.plusDays(4));
        form.setEndDate(monday.plusDays(7));
        CourseBatch batch = batches.save(null, form);
        assertEquals(2.0, batch.getTrainingDays());
        form.setHalfDayPeriod("AM");
        assertEquals(
                400,
                assertThrows(ResponseStatusException.class, () -> batches.save(null, form))
                        .getStatusCode()
                        .value());
    }

    @Test
    void usedScheduleKeepsDatesAndReservedCapacity() {
        CourseBatch batch = batches.save(null, batchForm());
        batchRepo.flush();
        addApplication(batch, ApplicationStatus.APPLIED);
        addApplication(batch, ApplicationStatus.APPROVED);
        var edit = batches.form(batch.getBatchId());
        edit.setStartDate(monday.plusDays(1));
        edit.setEndDate(monday.plusDays(1));
        assertEquals(
                400,
                assertThrows(
                                ResponseStatusException.class,
                                () -> batches.save(batch.getBatchId(), edit))
                        .getStatusCode()
                        .value());
        edit.setStartDate(monday);
        edit.setEndDate(monday);
        edit.setCapacity(1);
        assertEquals(
                400,
                assertThrows(
                                ResponseStatusException.class,
                                () -> batches.save(batch.getBatchId(), edit))
                        .getStatusCode()
                        .value());
        assertEquals(8, catalogue.schedules(course.getCourseId()).getFirst().places());
    }

    @Test
    void courseEditsDoNotRewriteApplicationSnapshotAndRemoveArchives() {
        CourseBatch batch = batches.save(null, batchForm());
        batchRepo.flush();
        CourseApplication application = addApplication(batch, ApplicationStatus.APPLIED);
        var edit = courses.form(course.getCourseId());
        edit.setCourseFee(new BigDecimal("900.00"));
        edit.setTitle("New title");
        courses.save(course.getCourseId(), edit);
        courseRepo.flush();
        assertEquals("Spring course", application.getCourseTitle());
        assertEquals(0, application.getCourseFee().compareTo(new BigDecimal("100.00")));
        courses.remove(course.getCourseId(), course.getVersion());
        courseRepo.flush();
        assertFalse(course.isActive());
        assertTrue(applications.existsById(application.getCourseId()));
        providers.remove(provider.getProviderId(), provider.getVersion());
        providerRepo.flush();
        assertFalse(provider.isActive());
    }

    @Test
    void calculatorPostNeverPersistsSchedule() throws Exception {
        long count = batchRepo.count();
        mvc.perform(
                        post("/admin/schedule")
                                .sessionAttr("user", admin)
                                .param("category", "EXTERNAL_COURSE")
                                .param("startDate", monday.toString())
                                .param("days", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Suggested dates")));
        assertEquals(count, batchRepo.count());
    }

    // Admin chooses a catalogue definition; employees later submit only dates and a reason.
    private CourseForm courseForm(int category) {
        var form = new CourseForm();
        form.setTitle("Spring course");
        form.setCourseDescription("Practice Java MVC");
        form.setCourseFee(new BigDecimal("100.00"));
        form.setCategoryId(category);
        form.setProviderId(provider.getProviderId());
        form.setCustomDatesAllowed(true);
        return form;
    }

    // A single working day keeps date failures separate from catalogue maintenance assertions.
    private CourseBatchForm batchForm() {
        var form = new CourseBatchForm();
        form.setCourseId(course.getCourseId());
        form.setStartDate(monday);
        form.setEndDate(monday);
        form.setCapacity(10);
        return form;
    }

    // A stored snapshot must survive changes to the current catalogue offer.
    private CourseApplication addApplication(CourseBatch batch, ApplicationStatus status) {
        var app = new CourseApplication();
        app.setApplicant(staff);
        app.setCourseTitle(course.getTitle());
        app.setCourseFee(course.getCourseFee());
        app.setCourseCategory(course.getCourseCategory().getKind());
        app.setTrainingProvider(provider.getName());
        app.setCourseStartDate(monday);
        app.setCourseEndDate(monday);
        app.setTrainingDays(1.0);
        app.setCatalogueCourse(course);
        app.setCatalogueBatch(batch);
        app.setStatus(status);
        return applications.saveAndFlush(app);
    }
}

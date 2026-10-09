package group6.project.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import group6.project.model.Admin;
import group6.project.model.ApprovalHierarchy;
import group6.project.model.CourseCategory;
import group6.project.model.CourseDetail;
import group6.project.model.ExcludedDays;
import group6.project.model.Manager;
import group6.project.model.Roles;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.service.AdminService;
import group6.project.service.CourseApplicationService;
import group6.project.service.CourseCategoryService;
import group6.project.service.CourseScheduleService;
import group6.project.service.CourseBatchService;
import group6.project.model.CourseBatch;
import group6.project.service.ExcludedDaysService;
import group6.project.repo.CourseCategoryRepository;

@WebMvcTest(controllers = {
        AdminController.class,
        CourseBatchController.class,
        CourseCategoryController.class,
        ExcludedDaysController.class
})
class AdminManagementTemplateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private CourseCategoryService courseCategoryService;

    @MockitoBean
    private CourseCategoryRepository courseCategoryRepository;

    @MockitoBean
    private CourseApplicationService courseApplicationService;

    @MockitoBean
    private ExcludedDaysService excludedDaysService;

    @MockitoBean
    private CourseScheduleService courseScheduleService;

    @MockitoBean
    private CourseBatchService courseBatchService;

    private Staff staff;
    private CourseCategory category;
    private CourseDetail course;
    private CourseBatch batch;
    private ApprovalHierarchy hierarchy;
    private ExcludedDays holiday;

    @BeforeEach
    void setUp() {
        staff = new Staff();
        staff.setUserId(11);
        staff.setName("Avery Staff");
        staff.setUserName("avery");
        staff.setStaffId("S0011");
        staff.setRole(Roles.STAFF);
        staff.setTrainingBudget(1800.0);
        staff.setTrainingDays(8);

        category = new CourseCategory();
        category.setCategoryId(4);
        category.setCategoryName("External Course");

        course = new CourseDetail();
        course.setCourseId(7);
        course.setTitle("Spring");
        course.setCourseFee(50.0);
        course.setCourseCategory(category);

        batch = new CourseBatch();
        batch.setBatchId(18L);
        batch.setCourseDetail(course);
        batch.setCourseStartDate(LocalDate.of(2026, 10, 12));
        batch.setCourseEndDate(LocalDate.of(2026, 10, 16));
        batch.setTrainingDays(5.0);
        batch.setCapacity(20);

        hierarchy = new ApprovalHierarchy();
        hierarchy.setHierarchyId(3);
        hierarchy.setLevel(1);
        hierarchy.setRole(Roles.MANAGER);

        holiday = new ExcludedDays();
        holiday.setId(2);
        holiday.setDate(LocalDate.of(2026, 12, 25));
        holiday.setDescription("Christmas Day");

        when(adminService.getAllStaff()).thenReturn(List.of(staff));
        when(adminService.getAllAdmins()).thenReturn(List.of(new Admin()));
        when(adminService.getManagerList()).thenReturn(List.of(new Manager()));
        when(adminService.getIdStaff(11)).thenReturn(Optional.of(staff));
        when(adminService.getAllApprovalHierarchy()).thenReturn(List.of(hierarchy));
        when(adminService.getHierarchyById(3)).thenReturn(Optional.of(hierarchy));
        when(adminService.getAllCourseDetails()).thenReturn(List.of(course));
        when(adminService.getByIdCourseDetails(7)).thenReturn(Optional.of(course));
        when(courseCategoryService.getAllCategories()).thenReturn(List.of(category));
        when(courseCategoryService.getCategoryById(4)).thenReturn(Optional.of(category));
        when(excludedDaysService.getAllExcludedDays()).thenReturn(List.of(holiday));
        when(excludedDaysService.getExcludedDayById(2)).thenReturn(holiday);
        when(courseScheduleService.generateCalendars(
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class)))
                .thenReturn(List.of(new CourseScheduleService.CalendarMonth("OCTOBER", 2026, List.of())));
        when(courseBatchService.getAllBatches()).thenReturn(List.of(batch));
        when(courseBatchService.getBatchById(18L)).thenReturn(Optional.of(batch));
    }

    @Test
    void adminManagementPagesRenderWithRowsAndConfirmationDialogs() throws Exception {
        mockMvc.perform(get("/admin/staffs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/calendar\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Course calendar")));
        mockMvc.perform(get("/admin/calendar"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Calculate a course schedule")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Course batch")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Spring (Batch 18, 5.0 days)")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("OCTOBER 2026")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cats-calendar-table")));
        mockMvc.perform(get("/admin/staffs/add")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/staffs/update/11")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/budgets")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/budgets/edit/11")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/hierarchy")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/hierarchy/edit/3")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/courses")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/courses/edit/7")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/categories/new")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/categories/edit/4")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/categories/delete")).andExpect(status().isOk());
        mockMvc.perform(get("/excluded-days")).andExpect(status().isOk());
        mockMvc.perform(get("/excluded-days/edit/2")).andExpect(status().isOk());
    }

    @Test
    void calendarExplainsHowToCreateFirstBatchWhenCatalogueHasNoBatches() throws Exception {
        when(courseBatchService.getAllBatches()).thenReturn(List.of());

        mockMvc.perform(get("/admin/calendar"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "There are no course batches to schedule.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "href=\"/admin/batches/new\"")));
    }

    @Test
    void courseBatchPagesUseAdminLayoutAndBatchFields() throws Exception {
        mockMvc.perform(get("/admin/batches"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Course batches")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Spring")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Training days")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-bs-toggle=\"modal\"")));

        mockMvc.perform(get("/admin/batches/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"course-batch-form\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"courseId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"trainingDays\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("name=\"courseStartDate\""))));

        mockMvc.perform(get("/admin/batches/edit/18"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Edit course batch")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"capacity\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("name=\"courseEndDate\""))));

        mockMvc.perform(get("/admin/batches/18"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Batch details")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("12 Oct 2026")));

        mockMvc.perform(get("/admin/batches/delete"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Delete course batch")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "action=\"/admin/batches/delete?batchId=18\"")));
    }

    @Test
    void accountFormOffersEveryRoleAndEmployeeFieldsAreRoleControlled() throws Exception {
        mockMvc.perform(get("/admin/staffs/add"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"ADMIN\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"MANAGER\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"STAFF\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"email\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-account-fields")));
    }

    @Test
    void adminEmailPageListsOnlyStaffAndManagersWithSendButtons() throws Exception {
        staff.setEmail("avery@example.com");
        Manager manager = new Manager();
        manager.setName("Morgan Manager");
        manager.setUserName("morgan");
        manager.setEmail("morgan@example.com");
        manager.setRole(Roles.MANAGER);
        Admin admin = new Admin();
        admin.setName("Admin User");
        admin.setUserName("admin");
        admin.setEmail("admin@example.com");
        admin.setRole(Roles.ADMIN);
        when(adminService.viewList()).thenReturn(List.of(staff, manager, admin));

        mockMvc.perform(get("/admin/emails"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/admin/login"));
        mockMvc.perform(get("/admin/emails").sessionAttr("user", admin))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("avery@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("morgan@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("admin@example.com"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Send email")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/emails\"")));
    }

    @Test
    void invalidFormSubmissionsRenderValidationFeedback() throws Exception {
        mockMvc.perform(post("/admin/accounts/create")).andExpect(status().isOk());
        mockMvc.perform(post("/admin/staffs/save")).andExpect(status().isOk());
        mockMvc.perform(post("/admin/budgets/save").param("userId", "11"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/admin/courses/save")).andExpect(status().isOk());
        mockMvc.perform(post("/admin/hierarchy/save")).andExpect(status().isOk());
        mockMvc.perform(post("/admin/categories")).andExpect(status().isOk());
        mockMvc.perform(post("/excluded-days/add")).andExpect(status().isOk());
        mockMvc.perform(post("/excluded-days/edit/2")).andExpect(status().isOk());
    }
}

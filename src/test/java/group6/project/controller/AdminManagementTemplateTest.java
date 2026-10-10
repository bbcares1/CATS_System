// We check that Admin pages render and their forms keep useful validation feedback.
package group6.project.controller;

import static group6.project.TestRequests.post;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import group6.project.form.AdminEmailForm;
import group6.project.model.Admin;
import group6.project.model.CourseBatch;
import group6.project.model.CourseCategory;
import group6.project.model.CourseDetail;
import group6.project.model.ExcludedDays;
import group6.project.model.Manager;
import group6.project.model.Roles;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.repo.CourseCategoryRepository;
import group6.project.service.AdminEmailService;
import group6.project.service.CourseApplicationService;
import group6.project.service.CourseBatchService;
import group6.project.service.CourseCategoryService;
import group6.project.service.CourseScheduleService;
import group6.project.service.ExcludedDaysService;
import group6.project.service.UserService;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;

@WebMvcTest(
        controllers = {
            AdminController.class,
            AccountAdminController.class,
            ExcludedDaysController.class
        })
class AdminManagementTemplateTest {
    @MockitoBean group6.project.service.TrainingEntitlementService entitlements;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService users;

    // These page tests isolate the service; LoginFlowTest checks saved identities.
    @BeforeEach
    void prepareIdentity() {
        when(users.currentUser(nullable(HttpSession.class)))
                .thenAnswer(
                        call -> {
                            HttpSession session = call.getArgument(0);
                            Object value = session == null ? null : session.getAttribute("user");
                            return value instanceof User user && user.getUserId() != null
                                    ? user
                                    : null;
                        });
    }

    @MockitoBean private group6.project.service.AccountAdminService accounts;

    @MockitoBean private AdminEmailService adminEmailService;

    @MockitoBean private CourseCategoryService courseCategoryService;

    @MockitoBean private CourseCategoryRepository courseCategoryRepository;

    @MockitoBean private CourseApplicationService courseApplicationService;

    @MockitoBean private ExcludedDaysService excludedDaysService;

    @MockitoBean private CourseScheduleService courseScheduleService;

    @MockitoBean private CourseBatchService courseBatchService;

    @Autowired private WebApplicationContext context;

    private Staff staff;
    private CourseCategory category;
    private CourseDetail course;
    private CourseBatch batch;
    private ExcludedDays holiday;

    @BeforeEach
    void setUp() {
        Admin admin = new Admin();
        admin.setUserId(99);
        admin.setName("Admin");
        admin.setUserName("admin");
        mockMvc =
                org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(
                                context)
                        .defaultRequest(get("/").sessionAttr("user", admin))
                        .build();
        staff = new Staff();
        staff.setUserId(11);
        staff.setName("Avery Staff");
        staff.setUserName("avery");
        staff.setStaffId("S0011");
        staff.setRole(Roles.STAFF);

        category = new CourseCategory();
        category.setCategoryId(4);
        category.setCategoryName("External Course");

        course = new CourseDetail();
        course.setCourseId(7);
        course.setTitle("Spring");
        course.setCourseFee(new java.math.BigDecimal("50.00"));
        course.setCourseCategory(category);

        batch = new CourseBatch();
        batch.setBatchId(18L);
        batch.setCourseDetail(course);
        batch.setCourseStartDate(LocalDate.of(2026, 10, 12));
        batch.setCourseEndDate(LocalDate.of(2026, 10, 16));
        batch.setTrainingDays(5.0);
        batch.setCapacity(20);

        holiday = new ExcludedDays();
        holiday.setId(2);
        holiday.setDate(LocalDate.of(2026, 12, 25));
        holiday.setDescription("Christmas Day");

        when(accounts.all()).thenReturn(List.of(staff));
        when(accounts.managers()).thenReturn(List.of());
        when(accounts.get(11)).thenReturn(staff);
        group6.project.form.AccountForm account = new group6.project.form.AccountForm();
        account.setName(staff.getName());
        account.setUserName(staff.getUserName());
        account.setStaffId(staff.getStaffId());
        account.setRole(Roles.STAFF);
        account.setVersion(0L);
        account.setEmail("avery@example.test");
        when(accounts.form(staff)).thenReturn(account);
        when(accounts.all()).thenReturn(List.of(staff));
        when(courseCategoryService.getAllCategories()).thenReturn(List.of(category));
        when(excludedDaysService.getAllExcludedDays()).thenReturn(List.of(holiday));
        when(excludedDaysService.getExcludedDayById(2)).thenReturn(holiday);
        when(entitlements.employee(11)).thenReturn(staff);
        var total =
                new group6.project.service.TrainingEntitlementService.AnnualSummary(
                        8,
                        new java.math.BigDecimal("1800"),
                        0,
                        java.math.BigDecimal.ZERO,
                        0,
                        java.math.BigDecimal.ZERO);
        when(entitlements.summary(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(total);
        when(entitlements.rows(org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(
                        List.of(
                                new group6.project.service.TrainingEntitlementService.AllowanceRow(
                                        staff, total)));
    }

    @Test
    void accountAllowanceAndHolidayPagesRender() throws Exception {
        for (String path :
                List.of(
                        "/admin/accounts",
                        "/admin/accounts/new",
                        "/admin/accounts/11/edit",
                        "/admin/budgets",
                        "/admin/budgets/edit/11",
                        "/admin/hierarchy",
                        "/excluded-days",
                        "/excluded-days/edit/2")) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    void dashboardGroupsActionsWithoutSeparateScheduleTools() throws Exception {
        mockMvc.perform(get("/admin/home"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Accounts and reporting managers")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "/admin/entitlements")))
                .andExpect(
                        content()
                                .string(org.hamcrest.Matchers.containsString("/admin/courses/new")))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("/admin/payments")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "/admin/schedule"))))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "Course batches"))));
    }

    @Test
    void accountFormOffersEveryRoleAndAnExplicitManagerId() throws Exception {
        mockMvc.perform(get("/admin/accounts/new"))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("value=\"ADMIN\"")))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("value=\"MANAGER\"")))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("value=\"STAFF\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"email\"")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "name=\"managerId\"")));
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
        admin.setUserId(99);
        admin.setName("Admin User");
        admin.setUserName("admin");
        admin.setEmail("admin@example.com");
        admin.setRole(Roles.ADMIN);
        when(accounts.all()).thenReturn(List.of(staff, manager, admin));

        org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .build()
                .perform(get("/admin/emails"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .redirectedUrl("/admin/login"));
        mockMvc.perform(get("/admin/emails").sessionAttr("user", admin))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("avery@example.com")))
                .andExpect(
                        content()
                                .string(org.hamcrest.Matchers.containsString("morgan@example.com")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "admin@example.com"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Send email")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "href=\"/admin/emails\"")));
    }

    @Test
    void emailComposerPrefillsRecipientAndSubmitsEditedAddress() throws Exception {
        Admin admin = new Admin();
        admin.setUserId(99);
        admin.setRole(Roles.ADMIN);
        mockMvc.perform(
                        get("/admin/emails")
                                .param("compose", "true")
                                .param("recipientEmail", "original@example.com")
                                .sessionAttr("user", admin))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "value=\"original@example.com\"")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "action=\"/admin/emails/send\"")));

        mockMvc.perform(
                        post("/admin/emails/send")
                                .sessionAttr("user", admin)
                                .param("recipientEmail", "edited@example.com")
                                .param("subject", "Course update")
                                .param("body", "Please review the new course schedule."))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .redirectedUrl("/admin/emails"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                                .attribute(
                                        "success",
                                        org.hamcrest.Matchers.containsString(
                                                "SMTP accepted the email for delivery")));

        org.mockito.ArgumentCaptor<AdminEmailForm> formCaptor =
                org.mockito.ArgumentCaptor.forClass(AdminEmailForm.class);
        verify(adminEmailService).send(formCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                "edited@example.com", formCaptor.getValue().getRecipientEmail());
    }

    @Test
    void emailSendFailureShowsConfigurationHintAndKeepsFormValues() throws Exception {
        Admin admin = new Admin();
        admin.setUserId(99);
        admin.setRole(Roles.ADMIN);
        doThrow(new org.springframework.mail.MailAuthenticationException("SMTP auth failure"))
                .when(adminEmailService)
                .send(org.mockito.ArgumentMatchers.any(AdminEmailForm.class));

        mockMvc.perform(
                        post("/admin/emails/send")
                                .sessionAttr("user", admin)
                                .param("recipientEmail", "recipient@example.com")
                                .param("subject", "Course update")
                                .param("body", "Please review the schedule."))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Check the mail profile and SMTP credentials")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "value=\"recipient@example.com\"")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "value=\"Course update\"")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Please review the schedule.")));
    }

    @Test
    void invalidFormSubmissionsRenderValidationFeedback() throws Exception {
        mockMvc.perform(post("/admin/accounts/save")).andExpect(status().isOk());
        mockMvc.perform(post("/admin/budgets/save").param("staffId", "11"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/excluded-days/add")).andExpect(status().isOk());
        mockMvc.perform(post("/excluded-days/edit/2")).andExpect(status().isOk());
    }
}

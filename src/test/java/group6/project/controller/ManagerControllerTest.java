package group6.project.controller;

import static org.mockito.ArgumentMatchers.nullable;

import jakarta.servlet.http.HttpSession;

import group6.project.service.UserService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import group6.project.model.Admin;
import group6.project.model.ApplicationStatus;
import group6.project.model.CourseCategoryType;
import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.service.ManagerService;
import group6.project.service.ManagerService.ApplicationGroup;
import group6.project.service.ManagerService.ApplicationView;

@WebMvcTest(ManagerController.class)
class ManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService users;

    // These page tests isolate the service; LoginFlowTest checks saved identities.
    @BeforeEach
    void prepareIdentity() {
        when(users.currentUser(
                nullable(HttpSession.class)))
                .thenAnswer(call -> {
                    HttpSession session = call.getArgument(0);
                    Object value = session == null ? null : session.getAttribute("user");
                    return value instanceof User user && user.getUserId() != null
                            ? user : null;
                });
    }

    @MockitoBean
    private ManagerService managerService;

    @Test
    void anonymousVisitorIsRedirectedToEmployeeLogin() throws Exception {
        mockMvc.perform(get("/manager/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/login"));
    }

    @ParameterizedTest
    @MethodSource("nonManagerUsers")
    void nonManagerSessionCannotOpenManagerWorkspace(Object user) throws Exception {
        mockMvc.perform(get("/manager/home").sessionAttr("user", user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/login"));
    }

    static Stream<Object> nonManagerUsers() {
        return Stream.of(new Staff(), new Admin(), "mgr_bob");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/manager", "/manager/home"})
    void signedInManagerReceivesRenderedDashboard(String path) throws Exception {
        Manager manager = new Manager();
        manager.setUserId(1);
        manager.setName("Bob & Team");
        manager.setStaffId("M001");

        mockMvc.perform(get(path).sessionAttr("user", manager))
                .andExpect(status().isOk())
                .andExpect(view().name("manager-home"))
                .andExpect(model().attribute("currentUser", manager))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Bob &amp; Team")))
                .andExpect(content().string(containsString("M001")))
                .andExpect(content().string(containsString("href=\"/staff/home\"")))
                .andExpect(content().string(containsString("href=\"/manager/approvals\"")))
                .andExpect(content().string(containsString("href=\"/logout\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/managers", "/api/managers/1", "/api/managers/staff-id/M001"})
    void retiredApiRoutesAreNotExposed(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/manager/approvals", "/manager/applications/10"})
    void newPagesRequireAManagerSession(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/login"));
        mockMvc.perform(get(path).sessionAttr("user", new Staff()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/login"));
        mockMvc.perform(get(path).sessionAttr("user", new Admin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/login"));
        mockMvc.perform(get(path).sessionAttr("user", new Manager()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/login"));
        verifyNoInteractions(managerService);
    }

    @Test
    void pendingApplicationsRenderAGroupedTableWithEscapedContent() throws Exception {
        var application = application(ApplicationStatus.APPLIED);
        var groups = List.of(new ApplicationGroup(2, "Alex & Team", "S002", List.of(application)));
        when(managerService.getPendingApplicationGroups(1)).thenReturn(groups);

        mockMvc.perform(get("/manager/approvals").param("managerId", "99")
                .sessionAttr("user", manager()))
                .andExpect(status().isOk())
                .andExpect(view().name("manager-approvals"))
                .andExpect(model().attribute("groups", groups))
                .andExpect(model().attribute("applicationCount", 1))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Alex &amp; Team")))
                .andExpect(content().string(containsString("Java &lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(containsString("href=\"/manager/applications/10\"")))
                .andExpect(content().string(containsString("$1,800.00")))
                .andExpect(content().string(containsString("12 Nov 2026")))
                .andExpect(content().string(containsString("cats-status-applied")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
        verify(managerService).getPendingApplicationGroups(1);
    }

    @Test
    void emptyPendingListShowsAnEmptyState() throws Exception {
        when(managerService.getPendingApplicationGroups(1)).thenReturn(List.of());

        mockMvc.perform(get("/manager/approvals").sessionAttr("user", manager()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No applications waiting for review")));
    }

    @Test
    void detailsRenderApplicationAndDecisionFieldsAsReadOnly() throws Exception {
        var application = application(ApplicationStatus.REJECTED);
        when(managerService.getApplicationForManager(1, 10)).thenReturn(application);

        mockMvc.perform(get("/manager/applications/10").sessionAttr("user", manager()))
                .andExpect(status().isOk())
                .andExpect(view().name("manager-application-detail"))
                .andExpect(model().attribute("courseApplication", application))
                .andExpect(content().string(containsString("Alex &amp; Team")))
                .andExpect(content().string(containsString("NUS-ISS")))
                .andExpect(content().string(containsString("Improve our system design.")))
                .andExpect(content().string(containsString("Share the learning with the team.")))
                .andExpect(content().string(containsString("Conflicts with a project deadline.")))
                .andExpect(content().string(containsString("cats-status-rejected")))
                .andExpect(content().string(not(containsString("<form"))));
    }

    @Test
    void missingOrOtherTeamApplicationReturns404() throws Exception {
        when(managerService.getApplicationForManager(1, 99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course application not found"));

        mockMvc.perform(get("/manager/applications/99").sessionAttr("user", manager()))
                .andExpect(status().isNotFound());
    }

    private Manager manager() {
        Manager manager = new Manager();
        manager.setUserId(1);
        manager.setName("Bob");
        manager.setStaffId("M001");
        return manager;
    }

    private ApplicationView application(ApplicationStatus status) {
        return new ApplicationView(10, 2, "Alex & Team", "S002",
                "Java <script>alert(1)</script>", CourseCategoryType.EXTERNAL_COURSE, "NUS-ISS",
                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 13), 2.0, null, new java.math.BigDecimal("1800.0"),
                "Improve our system design.", "Share the learning with the team.", status,
                LocalDateTime.of(2026, 10, 9, 10, 0), null,
                status == ApplicationStatus.REJECTED ? LocalDateTime.of(2026, 10, 9, 11, 0) : null,
                status == ApplicationStatus.REJECTED ? "Conflicts with a project deadline." : null, null);
    }
}

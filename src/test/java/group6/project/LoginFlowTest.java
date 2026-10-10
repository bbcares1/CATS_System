// We check login, role access and logout using saved accounts.
package group6.project;

import static group6.project.TestRequests.post;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import group6.project.form.AdminEmailForm;
import group6.project.model.Admin;
import group6.project.model.Manager;
import group6.project.model.Roles;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.repo.UserRepo;
import group6.project.service.AdminEmailService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class LoginFlowTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepo users;
    @Autowired AdminEmailService email;
    MockMvc mvc;

    // Use the real web configuration and a fresh, rolled-back account per test.
    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // Login identifies a Manager automatically and renews the existing session ID.
    @Test
    void managerCanOpenBothWorkspaces() throws Exception {
        User manager = account(new Manager(), Roles.MANAGER);
        MockHttpSession session = new MockHttpSession();
        String original = session.getId();
        mvc.perform(
                        post("/employee/login")
                                .session(session)
                                .param("userName", manager.getUserName())
                                .param("password", "demo123"))
                .andExpect(redirectedUrl("/manager/home"));
        assertNotEquals(original, session.getId());
        for (String path : new String[] {"/manager/home", "/staff/home", "/staff/personal"}) {
            mvc.perform(get(path).session(session)).andExpect(status().isOk());
        }
    }

    // An employee cannot use the Admin login or open another role's pages.
    @Test
    void roleBoundariesAreEnforced() throws Exception {
        User staff = account(new Staff(), Roles.STAFF);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", staff);
        mvc.perform(get("/manager/home").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/home").session(session)).andExpect(status().isForbidden());
        mvc.perform(
                        post("/admin/login")
                                .param("userName", staff.getUserName())
                                .param("password", "demo123"))
                .andExpect(view().name("admin-login"))
                .andExpect(model().attributeExists("error"));
    }

    // The existing Admin username field reaches the shared login service.
    @Test
    void adminLoginRendersAndAuthenticates() throws Exception {
        User admin = account(new Admin(), Roles.ADMIN);
        mvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(org.hamcrest.Matchers.containsString("name=\"userName\"")));
        mvc.perform(
                        post("/admin/login")
                                .param("userName", admin.getUserName())
                                .param("password", "demo123"))
                .andExpect(redirectedUrl("/admin/home"));
    }

    // Old URLs use the same role gate and deployment context as the main pages.
    @Test
    void oldRoutesAndContextPathRequireLogin() throws Exception {
        mvc.perform(get("/course-applications/new")).andExpect(redirectedUrl("/employee/login"));
        mvc.perform(get("/excluded-days")).andExpect(redirectedUrl("/admin/login"));
        mvc.perform(get("/cats/staff/home").contextPath("/cats"))
                .andExpect(redirectedUrl("/cats/employee/login"));
    }

    // A missing SMTP connection affects sending, not application startup.
    @Test
    void startsWithoutSmtpAndReportsUnavailableSending() {
        assertThrows(IllegalStateException.class, () -> email.send(new AdminEmailForm()));
    }

    // Keep these accounts independent of developer seed data.
    private User account(User user, Roles role) {
        user.setUserName("login_" + role.name());
        user.setPassword("demo123");
        user.setName("Login test");
        user.setRole(role);
        if (user instanceof Staff staff) {
            staff.setStaffId("LOGIN-" + role.name());
        }
        return users.saveAndFlush(user);
    }
}

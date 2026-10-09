package group6.project;

import static group6.project.support.MvcRequests.post;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import group6.project.model.*;
import group6.project.repo.UserRepo;

@SpringBootTest
@Transactional
class SessionAccessIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepo users;
    MockMvc mvc;
    Staff staff;
    Manager manager;

    // Use real persisted subtypes so the interceptor checks database identity, not just a session cast.
    @BeforeEach
    void prepare() {
        mvc=MockMvcBuilders.webAppContextSetup(context).build();
        staff=account(new Staff(),"access_staff");
        manager=account(new Manager(),"access_manager");
    }

    // Current and old prefixes are protected before a controller can read or write records.
    @Test
    void anonymousRoutesAndAdminWritesRequireTheCorrectLogin() throws Exception {
        for(String path:java.util.List.of("/staff/home","/manager/approvals","/course-applications","/course-fee-applications")) {
            mvc.perform(get(path)).andExpect(redirectedUrl("/employee/login"));
        }
        mvc.perform(get("/admin/entitlements")).andExpect(redirectedUrl("/admin/login"));
        mvc.perform(get("/excluded-days")).andExpect(redirectedUrl("/admin/login"));
        mvc.perform(post("/admin/courses/save").session(session(staff)).param("title","Forbidden course"))
                .andExpect(redirectedUrl("/admin/login"));
        mvc.perform(get("/staff/claims/99/receipt")).andExpect(status().isUnauthorized());
    }

    // Inherited Staff access must stay available while Staff cannot open the team workspace.
    @Test
    void managerHasBothWorkspacesAndStaffCannotEnterManagerPages() throws Exception {
        mvc.perform(get("/staff/apply").session(session(manager))).andExpect(status().isOk());
        mvc.perform(get("/manager/home").session(session(manager))).andExpect(status().isOk());
        mvc.perform(get("/manager/home").session(session(staff))).andExpect(redirectedUrl("/employee/login"));
        mvc.perform(get("/staff/course-applications").session(session(manager))).andExpect(redirectedUrl("/staff/personal"));
        mvc.perform(get("/course-fee-applications").session(session(manager))).andExpect(redirectedUrl("/staff/fee"));
        mvc.perform(post("/course-fee-applications").session(session(staff))).andExpect(status().isMethodNotAllowed());
    }

    // A browser with a deleted account's old session loses access on its next request.
    @Test
    void deletedAccountSessionReturnsToLogin() throws Exception {
        MockHttpSession session=session(staff);
        users.delete(staff); users.flush();
        mvc.perform(get("/staff/home").session(session)).andExpect(redirectedUrl("/employee/login"));
        assertNull(session.getAttribute("user"));
    }

    // Authentication must not retain the identifier of an earlier anonymous session.
    @Test
    void successfulLoginRotatesTheSessionId() throws Exception {
        MockHttpSession session=new MockHttpSession(); String previous=session.getId();
        mvc.perform(post("/employee/login").session(session).param("userName","access_staff")
                .param("password","test").param("designation","Staff"))
                .andExpect(redirectedUrl("/staff/home"));
        assertNotEquals(previous,session.getId());
    }

    // A posted role value cannot choose privileges; login uses the stored subtype.
    @Test
    void loginIgnoresPostedRoleSelection() throws Exception {
        mvc.perform(post("/employee/login").param("userName","access_manager")
                .param("password","test").param("designation","Staff"))
                .andExpect(redirectedUrl("/manager/home"));
        mvc.perform(post("/employee/login").param("userName","access_staff")
                .param("password","test").param("designation","Manager"))
                .andExpect(redirectedUrl("/staff/home"));
    }

    // These accounts have no other records, keeping deletion tests isolated.
    private <T extends Staff> T account(T user,String name) {
        user.setUserName(name); user.setStaffId(name); user.setName(name); user.setPassword("test");
        user.setRole(user instanceof Manager?Roles.MANAGER:Roles.STAFF);
        return users.saveAndFlush(user);
    }

    // The single session key is shared by every role and workspace.
    private MockHttpSession session(User user) {
        MockHttpSession session=new MockHttpSession(); session.setAttribute("user",user); return session;
    }
}

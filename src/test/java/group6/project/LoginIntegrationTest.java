package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import group6.project.model.*;
import group6.project.repo.UserRepo;
import group6.project.repo.CourseApplicationRepo;
import java.time.LocalDate;

@SpringBootTest
@Transactional
class LoginIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepo users;
    @Autowired CourseApplicationRepo applications;
    MockMvc mvc;

    // Exercise real repositories and templates rather than mocked authentication.
    @BeforeEach
    void prepare() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // The submitted field must match the actual Admin login form.
    @Test
    void adminFormAndLoginUseTheSameField() throws Exception {
        Admin admin = new Admin();
        saveAccount(admin, "admin_fixture", Roles.ADMIN);
        mvc.perform(get("/admin/login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"userName\"")));
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session).param("userName", "admin_fixture")
                .param("password", "password"))
                .andExpect(redirectedUrl("/admin/home"));
        mvc.perform(get("/admin/home").session(session)).andExpect(status().isOk());
    }

    // Manager login must not remove personal employee capabilities.
    @Test
    void managerCanUseBothWorkspacesAndTheSharedSessionResolver() throws Exception {
        Manager manager = new Manager();
        manager.setStaffId("FIXTURE_MGR");
        manager.setTrainingDays(10);
        manager.setTrainingBudget(2000d);
        saveAccount(manager, "manager_fixture", Roles.MANAGER);
        CourseApplication course = new CourseApplication();
        course.setApplicant(manager);
        course.setCourseTitle("Manager personal course");
        course.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);
        course.setCourseStartDate(LocalDate.now().withDayOfYear(1));
        course.setCourseEndDate(course.getCourseStartDate());
        course.setCourseFee(0);
        course.setStatus(ApplicationStatus.COMPLETED);
        applications.saveAndFlush(course);
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/employee/login").session(session).param("userName", "manager_fixture")
                .param("password", "password").param("designation", "Manager"))
                .andExpect(redirectedUrl("/manager/home"));
        for (String route : new String[]{"/manager/home", "/staff/apply", "/staff/personal", "/staff/course-applications"}) {
            mvc.perform(get(route).session(session)).andExpect(status().isOk());
        }
        assertInstanceOf(Manager.class, session.getAttribute("user"));
    }

    // Keep account fixtures valid under the same password and identity rules as the app.
    private void saveAccount(User user, String username, Roles role) {
        user.setUserName(username);
        user.setName("Login fixture");
        user.setRole(role);
        user.setPassword("password");
        users.saveAndFlush(user);
    }
}

package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import group6.project.config.CsrfProtection;
import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.*;

@SpringBootTest @Transactional
class FormSafetyIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepo users;
    @Autowired CourseApplicationRepo applications;
    @Autowired CourseApplicationService policy;
    @Autowired TrainingEntitlementService allowances;
    MockMvc mvc;

    // Use the real interceptors and template engine, including automatically inserted hidden fields.
    @BeforeEach void prepare() { mvc=MockMvcBuilders.webAppContextSetup(context).build(); }

    // Login writes and logout need a valid token; a GET bookmark does not end the session.
    @Test void loginAndLogoutRequireTheSessionFormToken() throws Exception {
        MockHttpSession session=new MockHttpSession();
        mvc.perform(get("/employee/login").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        String token=CsrfProtection.token(session);
        mvc.perform(post("/employee/login").session(session).param("userName","nobody").param("password","x")).andExpect(status().isForbidden());
        mvc.perform(post("/employee/login").session(session).param("_csrf","wrong").param("userName","nobody").param("password","x")).andExpect(status().isForbidden());
        mvc.perform(post("/employee/login").session(session).param("_csrf",token).param("userName","nobody").param("password","x")).andExpect(status().isOk());
        mvc.perform(get("/logout").session(session)).andExpect(status().is3xxRedirection());assertFalse(session.isInvalid());
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());assertFalse(session.isInvalid());
        mvc.perform(post("/logout").session(session).param("_csrf",token)).andExpect(status().is3xxRedirection());assertTrue(session.isInvalid());
    }

    // Authentication rotates both session identity and token, invalidating a previous login form.
    @Test void successfulLoginRotatesTheFormToken() throws Exception {
        account(new Staff(),"form_login");var session=new MockHttpSession();String oldToken=CsrfProtection.token(session);String oldId=session.getId();
        mvc.perform(post("/employee/login").session(session).param("_csrf",oldToken).param("userName","form_login").param("password","test"))
                .andExpect(status().is3xxRedirection());
        assertNotEquals(oldId,session.getId());assertNotEquals(oldToken,CsrfProtection.token(session));
        mvc.perform(post("/logout").session(session).param("_csrf",oldToken)).andExpect(status().isForbidden());
        assertFalse(session.isInvalid());
    }

    // Two tabs cannot silently overwrite an application edited in the other tab.
    @Test void staleOtherCourseEditDoesNotOverwriteTheSavedDetails() {
        Manager manager=account(new Manager(),"form_manager");Staff employee=new Staff();employee.setManager(manager);employee=account(employee,"form_staff");
        LocalDate date=LocalDate.now().plusDays(7).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        allowances.saveLimits(employee.getUserId(),date.getYear(),10,new BigDecimal("1000"));
        var first=form(date);var course=policy.create(first,employee);applications.flush();Long original=course.getVersion();
        var edit=form(date);edit.setVersion(original);edit.setCourseTitle("Newer saved title");policy.update(course.getCourseId(),edit,employee);applications.flush();
        var stale=form(date);stale.setVersion(original);stale.setCourseTitle("Stale title");
        Staff actor=employee;assertThrows(IllegalStateException.class,()->policy.update(course.getCourseId(),stale,actor));
        assertEquals("Newer saved title",applications.findById(course.getCourseId()).orElseThrow().getCourseTitle());
    }

    // A signed-in account still cannot submit a protected mutation from another site.
    @Test void protectedPostWithoutTokenNeverCallsTheBusinessHandler() throws Exception {
        Admin admin=account(new Admin(),"form_admin");MockHttpSession session=new MockHttpSession();session.setAttribute("user",admin);
        mvc.perform(post("/admin/excludedDays/add").session(session).param("date","2027-01-01").param("description","Forged"))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/staff/fee").session(session)).andExpect(status().isForbidden());
    }

    // Large Manager lists remain grouped and keep the selected employee when paging history.
    @Test void managerListsPaginateWithoutLosingTeamScope() throws Exception {
        Manager manager=account(new Manager(),"page_manager");Staff employee=new Staff();employee.setManager(manager);employee=account(employee,"page_employee");
        LocalDate date=LocalDate.of(LocalDate.now().getYear(),3,3);
        for(int i=0;i<31;i++) {var course=form(date.plusDays(i));course.setApplicant(employee);course.setApprovalManager(manager);course.setTrainingDays(1d);course.setStatus(ApplicationStatus.APPLIED);applications.save(course);}
        applications.flush();var session=new MockHttpSession();session.setAttribute("user",manager);
        var result=mvc.perform(get("/manager/approvals").session(session).param("page","1").param("size","20"))
                .andExpect(status().isOk()).andExpect(model().attribute("applicationCount",31)).andReturn();
        var groups=(java.util.List<ManagerService.ApplicationGroup>)result.getModelAndView().getModel().get("groups");
        assertEquals(11,groups.stream().mapToInt(g->g.applications().size()).sum());
        mvc.perform(get("/manager/history").session(session).param("employeeId",employee.getUserId().toString()).param("page","1").param("size","20"))
                .andExpect(status().isOk()).andExpect(model().attribute("courses",org.hamcrest.Matchers.hasSize(11)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"employeeId\"")));
    }

    // Test requests use realistic business fields; only their stale version differs.
    private CourseApplication form(LocalDate date) {
        var form=new CourseApplication();form.setCourseTitle("Original title");form.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);
        form.setTrainingProvider("ISS");form.setCourseStartDate(date);form.setCourseEndDate(date);form.setCourseFee(new BigDecimal("100"));form.setJustification("Improve skills");return form;
    }

    // Unique identities avoid depending on development seed accounts.
    private <T extends User> T account(T user,String name) {user.setUserName(name);user.setStaffId(name);user.setName(name);user.setPassword("test");return users.saveAndFlush(user);}
}

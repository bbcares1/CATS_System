package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import group6.project.model.*;
import group6.project.form.AccountForm;
import group6.project.repo.*;
import group6.project.service.*;

@SpringBootTest
@Transactional
class AccountAdministrationIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepo users;
    @Autowired StaffRepo employees;
    @Autowired CourseApplicationRepo applications;
    @Autowired CourseFeeApplicationRepo claims;
    @Autowired TrainingEntitlementRepo entitlements;
    @Autowired AccountAdminService accounts;
    @Autowired UserService login;
    Admin actor; MockMvc mvc;

    // Actual account subtypes exercise role changes, not just a changed enum.
    @BeforeEach
    void prepare() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        actor = new Admin(); actor.setName("Account admin"); actor.setUserName("account_admin"); actor.setPassword("test");
        actor.setStaffId("ADMIN-TEST"); actor = users.saveAndFlush(actor);
    }

    // Promotion changes the persisted Java type while preserving annual, application and claim identity.
    @Test
    void roleChangesKeepHistoryAndAllowancesOnTheSameUserId() {
        User employee = accounts.save(null, form("account_staff", Roles.STAFF), actor.getUserId());
        Integer id = employee.getUserId();
        TrainingEntitlement allowance = new TrainingEntitlement(LocalDate.now().getYear());
        allowance.setStaff(employee); allowance.setDayLimit(10d); allowance.setBudget(new BigDecimal("1000.00"));
        entitlements.saveAndFlush(allowance);
        CourseApplication course = historical(employee); course.setReviewer(actor); course = applications.saveAndFlush(course);
        CourseFeeApplication claim = new CourseFeeApplication(); claim.setApplicant(employee); claim.setCourseApplication(course);
        claim.setApplicationStatus(ApplicationStatus.APPROVED); claim = claims.saveAndFlush(claim);
        AccountForm promoted = accounts.form(employee); promoted.setRole(Roles.MANAGER);
        User manager = accounts.save(id, promoted, actor.getUserId()); assertInstanceOf(Manager.class, manager);
        AccountForm admin = accounts.form(manager); admin.setRole(Roles.ADMIN);
        User changed = accounts.save(id, admin, actor.getUserId()); assertInstanceOf(Admin.class, changed);
        assertEquals(id, applications.findById(course.getCourseId()).orElseThrow().getApplicant().getUserId());
        assertInstanceOf(Admin.class, applications.findById(course.getCourseId()).orElseThrow().getApplicant());
        assertInstanceOf(Admin.class, claims.findById(claim.getApplicationId()).orElseThrow().getApplicant());
        assertEquals(new BigDecimal("1000.00"), entitlements.findByStaff_UserIdAndYear(id, LocalDate.now().getYear()).orElseThrow().getBudget());
        assertTrue(employees.findById(id).isEmpty());
        AccountForm staffAgain = accounts.form(changed); staffAgain.setRole(Roles.STAFF);
        assertInstanceOf(Staff.class, accounts.save(id, staffAgain, actor.getUserId()));
        assertEquals(id, claims.findById(claim.getApplicationId()).orElseThrow().getApplicant().getUserId());
    }

    // Old decision records remain readable when their reviewer is no longer a Manager.
    @Test
    void demotingAReviewerKeepsTheApprovalRecord() {
        User manager = accounts.save(null, form("former_manager", Roles.MANAGER), actor.getUserId());
        User employee = accounts.save(null, form("reviewed_employee", Roles.STAFF), actor.getUserId());
        CourseApplication course = historical(employee); course.setReviewer(manager); course = applications.saveAndFlush(course);
        AccountForm demote = accounts.form(manager); demote.setRole(Roles.STAFF); accounts.save(manager.getUserId(), demote, actor.getUserId());
        CourseApplication loaded = applications.findById(course.getCourseId()).orElseThrow();
        assertEquals(manager.getUserId(), loaded.getReviewer().getUserId()); assertInstanceOf(Staff.class, loaded.getReviewer());
        assertEquals("Previous decision", loaded.getDecisionReason());
    }

    // Reassign reports before removing Manager access, and never allow a self/long reporting cycle.
    @Test
    void assignmentsRejectCyclesAndUnsafeManagerDemotion() {
        User boss = accounts.save(null, form("cycle_boss", Roles.MANAGER), actor.getUserId());
        AccountForm subordinate = form("cycle_subordinate", Roles.MANAGER); subordinate.setManagerId(boss.getUserId());
        User report = accounts.save(null, subordinate, actor.getUserId());
        AccountForm self = accounts.form(boss); self.setManagerId(boss.getUserId());
        assertThrows(ResponseStatusException.class, () -> accounts.save(boss.getUserId(), self, actor.getUserId()));
        AccountForm cycle = accounts.form(boss); cycle.setManagerId(report.getUserId());
        assertThrows(ResponseStatusException.class, () -> accounts.save(boss.getUserId(), cycle, actor.getUserId()));
        AccountForm demote = accounts.form(boss); demote.setRole(Roles.STAFF);
        assertThrows(ResponseStatusException.class, () -> accounts.save(boss.getUserId(), demote, actor.getUserId()));
    }

    // Disabled identities and password/role edits revoke old sessions on the very next request.
    @Test
    void disablingAndRoleChangesInvalidateExistingSessions() {
        User employee = accounts.save(null, form("session_employee", Roles.STAFF), actor.getUserId());
        MockHttpSession previous = session(employee); AccountForm change = accounts.form(employee); change.setRole(Roles.MANAGER);
        User manager = accounts.save(employee.getUserId(), change, actor.getUserId());
        assertNull(login.currentUser(previous)); assertNull(previous.getAttribute("user"));
        assertInstanceOf(Manager.class, login.authenticate("session_employee", "demo123"));
        MockHttpSession managerSession = session(manager); AccountForm disabled = accounts.form(manager); disabled.setActive(false);
        accounts.save(manager.getUserId(), disabled, actor.getUserId());
        assertNull(login.currentUser(managerSession)); assertNull(login.authenticate("session_employee", "demo123"));
    }

    // Delete works for an unused mistake but cannot cascade historical records or remove the acting Admin.
    @Test
    void unusedAccountsCanBeDeletedWhileHistoryAndSelfAccessAreProtected() {
        User unused = accounts.save(null, form("unused_account", Roles.STAFF), actor.getUserId());
        accounts.delete(unused.getUserId(), unused.getVersion(), actor.getUserId()); assertTrue(users.findById(unused.getUserId()).isEmpty());
        User used = accounts.save(null, form("used_account", Roles.STAFF), actor.getUserId()); applications.saveAndFlush(historical(used));
        assertThrows(ResponseStatusException.class, () -> accounts.delete(used.getUserId(), used.getVersion(), actor.getUserId()));
        assertThrows(ResponseStatusException.class, () -> accounts.delete(actor.getUserId(), actor.getVersion(), actor.getUserId()));
        AccountForm self = accounts.form(actor); self.setActive(false);
        assertThrows(ResponseStatusException.class, () -> accounts.save(actor.getUserId(), self, actor.getUserId()));
    }

    // A blank edit password retains the current value; stale versions and duplicate identities are rejected.
    @Test
    void editPasswordsStayPrivateAndStaleDuplicateFormsFail() throws Exception {
        User employee = accounts.save(null, form("edit_account", Roles.STAFF), actor.getUserId());
        AccountForm edit = accounts.form(employee); assertNull(edit.getPassword()); edit.setDesignation("Engineer");
        User updated = accounts.save(employee.getUserId(), edit, actor.getUserId()); assertEquals("demo123", updated.getPassword());
        edit.setVersion(-1L); assertThrows(ResponseStatusException.class, () -> accounts.save(employee.getUserId(), edit, actor.getUserId()));
        assertThrows(ResponseStatusException.class, () -> accounts.save(null, form("EDIT_ACCOUNT", Roles.STAFF), actor.getUserId()));
        String html = mvc.perform(get("/admin/accounts/" + employee.getUserId() + "/edit").session(session(actor)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("demo123"));
        mvc.perform(get("/admin/accounts").session(session(actor))).andExpect(status().isOk());
        mvc.perform(get("/admin/accounts/new").session(session(actor))).andExpect(status().isOk());
        mvc.perform(post("/admin/accounts/save").session(session(actor)).param("userName", ""))
                .andExpect(status().isOk()).andExpect(model().hasErrors());
        mvc.perform(get("/admin/staffs").session(session(actor))).andExpect(redirectedUrl("/admin/accounts"));
        mvc.perform(get("/admin/staffs/add").session(session(actor))).andExpect(redirectedUrl("/admin/accounts/new"));
        mvc.perform(get("/admin/staffs/update/" + employee.getUserId()).session(session(actor)))
                .andExpect(redirectedUrl("/admin/accounts/" + employee.getUserId() + "/edit"));
    }

    // Open employee work must be resolved before removing its owner's employee access.
    @Test
    void pendingRequestsPreventConversionToAdminOrDisabling() {
        User employee = accounts.save(null, form("pending_employee", Roles.STAFF), actor.getUserId());
        CourseApplication request = historical(employee); request.setStatus(ApplicationStatus.APPLIED); applications.saveAndFlush(request);
        AccountForm change = accounts.form(employee); change.setRole(Roles.ADMIN);
        assertThrows(ResponseStatusException.class, () -> accounts.save(employee.getUserId(), change, actor.getUserId()));
        change.setRole(Roles.STAFF); change.setActive(false);
        assertThrows(ResponseStatusException.class, () -> accounts.save(employee.getUserId(), change, actor.getUserId()));
    }

    // Historical fixtures need no future eligibility checks because they represent already completed work.
    private CourseApplication historical(User employee) {
        CourseApplication course = new CourseApplication(); course.setApplicant(employee); course.setStatus(ApplicationStatus.COMPLETED);
        course.setCourseTitle("Historical course"); course.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);
        course.setTrainingProvider("Training centre"); course.setCourseStartDate(LocalDate.now().minusDays(20));
        course.setCourseEndDate(LocalDate.now().minusDays(20)); course.setCourseFee(new java.math.BigDecimal("100"));
        course.setTrainingDays(1d); course.setJustification("Develop skills"); course.setDecisionReason("Previous decision"); return course;
    }

    // Every Admin-created fixture has a unique, simple username and employee identifier.
    private AccountForm form(String name, Roles role) {
        AccountForm form = new AccountForm(); form.setUserName(name); form.setName(name); form.setStaffId(name);
        form.setPassword("demo123"); form.setRole(role); form.setEmail(name.toLowerCase() + "@example.test"); return form;
    }

    // Login uses one shared session key for every subtype.
    private MockHttpSession session(User user) { MockHttpSession session = new MockHttpSession(); session.setAttribute("user", user); return session; }
}

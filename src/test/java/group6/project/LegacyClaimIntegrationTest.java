package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.*;

@SpringBootTest
@Transactional
class LegacyClaimIntegrationTest {
    @Autowired UserRepo users;
    @Autowired CourseApplicationRepo applications;
    @Autowired CourseFeeApplicationRepo claims;
    @Autowired CourseFeeApplicationService service;
    @Autowired ReviewAssignmentService assignments;
    @Autowired CourseDetailRepo courses;
    @Autowired CourseBatchRepo batches;
    @Autowired WebApplicationContext context;
    Staff employee; Manager reviewer; Admin admin; MockMvc mvc;

    // Legacy records are created without reviewers to exercise the actual upgrade repair path.
    @BeforeEach void prepare() {
        employee=account(new Staff(),"legacy_employee");reviewer=account(new Manager(),"legacy_manager");admin=account(new Admin(),"legacy_admin");
        mvc=MockMvcBuilders.webAppContextSetup(context).build();
    }

    // Repair existing unassigned claims/applications without recreating records or guessing a reviewer.
    @Test void adminCanAssignExistingPendingRequests() throws Exception {
        var course=application();var claim=claim(course);var pending=application();pending.setStatus(ApplicationStatus.APPLIED);applications.saveAndFlush(pending);
        mvc.perform(get("/admin/reviews").session(session(admin))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Unassigned fee claims")));
        assignments.assign("claim",claim.getApplicationId(),reviewer.getUserId(),claim.getVersion(),admin.getUserId());
        assignments.assign("application",pending.getCourseId(),reviewer.getUserId(),pending.getVersion(),admin.getUserId());
        assertEquals(claim.getApplicationId(),service.pending(reviewer.getUserId(),0,10).getContent().getFirst().getApplicationId());
        assertSame(reviewer,pending.getApprovalManager());
        service.decide(claim.getApplicationId(),reviewer.getUserId(),"approve","Old evidence checked",claim.getVersion());
        assertEquals(ApplicationStatus.APPROVED,claim.getApplicationStatus());
    }

    // Review assignment cannot overwrite a current route, accept stale forms or enable self-review.
    @Test void repairProtectsCurrentAssignmentsAndVersions() {
        var claim=claim(application());
        assertThrows(IllegalArgumentException.class,()->assignments.assign("claim",claim.getApplicationId(),reviewer.getUserId(),99L,admin.getUserId()));
        assignments.assign("claim",claim.getApplicationId(),reviewer.getUserId(),claim.getVersion(),admin.getUserId());claims.flush();
        assertThrows(IllegalArgumentException.class,()->assignments.assign("claim",claim.getApplicationId(),reviewer.getUserId(),claim.getVersion(),admin.getUserId()));
    }

    // A top-level manager's old claim cannot be assigned back to themselves, or repaired by Staff.
    @Test void repairEnforcesAdminAndNoSelfReview() {
        var course=application();course.setApplicant(reviewer);applications.saveAndFlush(course);
        var claim=claim(course);claim.setApplicant(reviewer);claims.saveAndFlush(claim);
        assertThrows(IllegalArgumentException.class,()->assignments.assign("claim",claim.getApplicationId(),reviewer.getUserId(),claim.getVersion(),admin.getUserId()));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->assignments.assign("claim",claim.getApplicationId(),reviewer.getUserId(),claim.getVersion(),employee.getUserId()));
        assertNull(claim.getApprovalManager());
    }

    // Lists page small metadata results, while the large evidence remains available only on its private route.
    @Test void claimListsArePagedWithoutEvidence() throws Exception {
        for(int i=0;i<31;i++) {var claim=claim(application());claim.setApprovalManager(reviewer);claims.save(claim);}
        claims.flush();var page=service.pending(reviewer.getUserId(),1,20);
        assertEquals(31,page.getTotalElements());assertEquals(11,page.getNumberOfElements());assertEquals(20,page.getSize());
        mvc.perform(get("/manager/claims").session(session(reviewer)).param("page","1").param("size","20"))
                .andExpect(status().isOk()).andExpect(model().attribute("claims",org.hamcrest.Matchers.hasSize(11)));
        mvc.perform(get("/staff/fee").session(session(employee)).param("page","1").param("size","20"))
                .andExpect(status().isOk()).andExpect(model().attribute("claims",org.hamcrest.Matchers.hasSize(11)));
    }

    // Batch-only historical payments still belong in their owner's reimbursement total.
    @Test void legacyBatchPaymentAppearsInPersonalTotals() throws Exception {
        var offer=new CourseDetail();offer.setTitle("Old paid course");offer.setCourseFee(new BigDecimal("200"));offer=courses.saveAndFlush(offer);
        var batch=new CourseBatch();batch.setCourseDetail(offer);batch.setCourseStartDate(LocalDate.now().minusDays(20));batch.setCourseEndDate(batch.getCourseStartDate());batch=batches.saveAndFlush(batch);
        var claim=new CourseFeeApplication();claim.setApplicant(employee);claim.setCourseBatch(batch);claim.setAmount(new BigDecimal("200"));
        claim.setApplicationStatus(ApplicationStatus.APPROVED);claim=claims.saveAndFlush(claim);
        service.reimburse(claim.getApplicationId(),admin.getUserId(),"LEGACY-PAY",claim.getVersion());claims.flush();
        assertEquals(0,new BigDecimal("200").compareTo(service.reimbursed(employee,LocalDate.now().getYear())));
        assertTrue(service.approved(0,10).getContent().getFirst().getLegacyClaim());
        mvc.perform(get("/admin/payments").session(session(admin))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("verify the migrated fee")));
    }

    // Persisted evidence and application ownership are required before an old claim can be approved.
    private CourseFeeApplication claim(CourseApplication course) {
        var claim=new CourseFeeApplication();claim.setApplicant(employee);claim.setCourseApplication(course);claim.setAmount(new BigDecimal("200"));
        claim.setApplicationStatus(ApplicationStatus.APPLIED);claim.setReceipt("%PDF-1.4".getBytes());claim.setCertificate("%PDF-1.4".getBytes());
        return claims.saveAndFlush(claim);
    }
    private CourseApplication application() {
        var course=new CourseApplication();course.setApplicant(employee);course.setCourseTitle("Legacy training");course.setStatus(ApplicationStatus.COMPLETED);
        course.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);course.setCourseStartDate(LocalDate.now().minusDays(20));
        course.setCourseEndDate(course.getCourseStartDate());course.setCourseFee(new BigDecimal("200"));return applications.saveAndFlush(course);
    }
    private <T extends User> T account(T user,String name) {user.setUserName(name);user.setName(name);user.setStaffId(name);user.setPassword("test");return users.saveAndFlush(user);}
    private MockHttpSession session(User user) {var session=new MockHttpSession();session.setAttribute("user",user);return session;}
}

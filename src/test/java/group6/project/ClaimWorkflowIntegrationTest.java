package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.*;

@SpringBootTest
@Transactional
class ClaimWorkflowIntegrationTest {
    @Autowired UserRepo users;
    @Autowired CourseApplicationRepo applications;
    @Autowired CourseFeeApplicationRepo claims;
    @Autowired CourseFeeApplicationService service;
    @Autowired CourseApplicationService policy;
    @Autowired ManagerService managers;
    @Autowired TrainingEntitlementService allowances;
    @Autowired AccountAdminService accounts;
    @Autowired WebApplicationContext context;
    Staff employee; Manager boss,peer; Admin admin; CourseApplication course; MockMvc mvc;

    // Persist real roles and reviewer assignments, with automatic rollback after every case.
    @BeforeEach void prepare() {
        boss=account(new Manager(),"claim_boss"); peer=account(new Manager(),"claim_peer"); admin=account(new Admin(),"claim_admin");
        employee=new Staff(); employee.setManager(boss); employee=account(employee,"claim_employee");
        course=completed(employee); mvc=MockMvcBuilders.webAppContextSetup(context).build();
    }

    // Staff submit, the manager decides and Admin records the actual payment as separate events.
    @ParameterizedTest @ValueSource(strings={"approve","reject"})
    void completeClaimFlowKeepsApprovalSeparateFromPayment(String decision) throws Exception {
        var claim=submit(employee,null); Long old=claim.getVersion();
        service.decide(claim.getApplicationId(),boss.getUserId(),decision,"Receipt checked",old); claims.flush();
        assertEquals("approve".equals(decision)?ApplicationStatus.APPROVED:ApplicationStatus.REJECTED,claim.getApplicationStatus());
        assertSame(boss,claim.getReviewer()); assertNull(claim.getReimbursedAt());
        assertEquals(0,service.reimbursed(employee,LocalDate.now().getYear()).compareTo(BigDecimal.ZERO));
        if ("approve".equals(decision)) {
            assertThrows(IllegalArgumentException.class,()->service.reimburse(claim.getApplicationId(),admin.getUserId(),"TX-1",old));
            service.reimburse(claim.getApplicationId(),admin.getUserId(),"TX-1",claim.getVersion()); claims.flush();
            assertNotNull(claim.getReimbursedAt()); assertSame(admin,claim.getReimbursedBy());
            assertEquals(new BigDecimal("123.45"),service.reimbursed(employee,LocalDate.now().getYear()));
            assertThrows(IllegalArgumentException.class,()->service.reimburse(claim.getApplicationId(),admin.getUserId(),"TX-2",claim.getVersion()));
        } else assertThrows(IllegalArgumentException.class,()->service.reimburse(claim.getApplicationId(),admin.getUserId(),"TX-1",claim.getVersion()));
        mvc.perform(get("/staff/claims/"+claim.getApplicationId()).session(session(employee))).andExpect(status().isOk());
        mvc.perform(get("/manager/claims/"+claim.getApplicationId()).session(session(boss))).andExpect(status().isOk());
        mvc.perform(get("/admin/payments").session(session(admin))).andExpect(status().isOk());
    }

    // Invalid or duplicate submissions leave the claim table unchanged.
    @Test void eligibilityAndDuplicateChecks() {
        assertThrows(IllegalArgumentException.class,()->service.submit(course.getCourseId(),false,pdf(),pdf(),employee,null));
        course.setStatus(ApplicationStatus.APPROVED); assertThrows(IllegalArgumentException.class,()->submit(employee,null));
        course.setStatus(ApplicationStatus.COMPLETED); course.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING);
        assertThrows(IllegalArgumentException.class,()->submit(employee,null));
        course.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE); submit(employee,null);
        assertThrows(IllegalArgumentException.class,()->submit(employee,null)); assertEquals(1,claims.count());
    }

    // Declared images cannot hide PDFs or executable content, and oversized uploads are rejected.
    @Test void invalidUploadsAreRejected() {
        for (MockMultipartFile bad:java.util.List.of(new MockMultipartFile("receipt","fake.jpg","image/jpeg","%PDF-1.4".getBytes()),
                new MockMultipartFile("receipt","script.pdf","application/pdf","<script>bad</script>".getBytes()),
                new MockMultipartFile("receipt","large.pdf","application/pdf",new byte[5*1024*1024+1]))) {
            assertThrows(IllegalArgumentException.class,()->service.submit(course.getCourseId(),true,bad,pdf(),employee,null));
        }
        assertEquals(0,claims.count());
    }

    // Review reasons, current versions and actor scope all apply before a decision is saved.
    @Test void invalidAndRepeatedDecisionsAreRejected() {
        var claim=submit(employee,null);
        assertThrows(IllegalArgumentException.class,()->service.decide(claim.getApplicationId(),boss.getUserId(),"approve"," ",claim.getVersion()));
        assertThrows(IllegalArgumentException.class,()->service.decide(claim.getApplicationId(),boss.getUserId(),"approve","Good",99L));
        assertThrows(ResponseStatusException.class,()->service.decide(claim.getApplicationId(),peer.getUserId(),"approve","Good",claim.getVersion()));
        service.decide(claim.getApplicationId(),boss.getUserId(),"approve","Good",claim.getVersion()); claims.flush();
        assertThrows(IllegalArgumentException.class,()->service.decide(claim.getApplicationId(),boss.getUserId(),"reject","Other",claim.getVersion()));
    }

    // Only the applicant and assigned manager can download evidence; Admin's payment page does not expose it.
    @Test void documentPrivacyAndQueueRendering() throws Exception {
        var claim=submit(employee,null); String path="/manager/claims/"+claim.getApplicationId();
        mvc.perform(get("/manager/claims").session(session(boss))).andExpect(status().isOk());
        mvc.perform(get(path+"/receipt").session(session(boss))).andExpect(status().isOk()).andExpect(content().bytes(pdf().getBytes()));
        mvc.perform(get(path+"/receipt").session(session(peer))).andExpect(status().isNotFound());
        mvc.perform(get(path+"/receipt")).andExpect(status().isUnauthorized());
        mvc.perform(get("/staff/claims/"+claim.getApplicationId()+"/receipt").session(session(peer))).andExpect(status().isNotFound());
    }

    // An unassigned Manager chooses another Manager for personal courses and claims, never themselves.
    @Test void peerReviewWorksWithoutReportingCycles() throws Exception {
        applications.delete(course); applications.flush(); course=completed(boss);
        assertThrows(IllegalArgumentException.class,()->submit(boss,boss.getUserId()));
        var claim=submit(boss,peer.getUserId());
        assertThrows(ResponseStatusException.class,()->service.decide(claim.getApplicationId(),boss.getUserId(),"approve","Self",claim.getVersion()));
        service.decide(claim.getApplicationId(),peer.getUserId(),"approve","Peer review",claim.getVersion());
        assertNull(boss.getManager());
        allowances.saveLimits(boss.getUserId(),LocalDate.now().getYear(),10,new BigDecimal("2000"));
        CourseApplication request=new CourseApplication(); LocalDate date=LocalDate.now().plusDays(7); while(date.getDayOfWeek().getValue()>5) date=date.plusDays(1);
        request.setCourseTitle("Peer course"); request.setTrainingProvider("ISS"); request.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);
        request.setCourseStartDate(date); request.setCourseEndDate(date); request.setJustification("Learn"); request.setApprovalManagerId(peer.getUserId());
        var application=policy.create(request,boss);
        assertTrue(managers.getPendingApplicationGroups(peer.getUserId()).stream().anyMatch(g->g.employeeId().equals(boss.getUserId())));
        assertEquals(boss.getUserId(),managers.getApplicationForManager(peer.getUserId(),application.getCourseId()).applicantId());
        managers.decide(peer.getUserId(),application.getCourseId(),"approve","Peer review",application.getVersion());
        mvc.perform(get("/staff/apply/other").session(session(boss))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Reviewing manager")));
    }

    // Existing requests keep their assigned reviewer when Admin changes the reporting relationship.
    @Test void reportingChangesDoNotMovePendingClaims() {
        var claim=submit(employee,null); employee.setManager(peer); users.saveAndFlush(employee);
        assertEquals(boss.getUserId(),claim.getApprovalManager().getUserId());
        assertEquals(1,service.pending(boss.getUserId(),0,10).getTotalElements()); assertTrue(service.pending(peer.getUserId(),0,10).isEmpty());
    }

    // An incomplete legacy claim can be rejected with a reason, but cannot be approved or paid.
    @Test void incompleteLegacyClaimCanBeRejected() {
        var claim=submit(employee,null); claim.setCertificate(null); claims.saveAndFlush(claim);
        assertThrows(IllegalArgumentException.class,()->service.decide(claim.getApplicationId(),boss.getUserId(),"approve","Checked",claim.getVersion()));
        service.decide(claim.getApplicationId(),boss.getUserId(),"reject","Missing completion evidence",claim.getVersion());
        assertEquals(ApplicationStatus.REJECTED,claim.getApplicationStatus());
    }

    // Assigned peer reviews also protect a manager from being demoted before the queue is resolved.
    @Test void pendingPeerReviewPreventsRemovingManagerAccess() {
        applications.delete(course); applications.flush(); course=completed(boss); submit(boss,peer.getUserId());
        var form=accounts.form(peer); form.setRole(Roles.STAFF);
        assertThrows(IllegalArgumentException.class,()->accounts.save(peer.getUserId(),form,admin.getUserId()));
        assertInstanceOf(Manager.class,users.findById(peer.getUserId()).orElseThrow());
    }

    // Foreign catalogue-edit URLs must return 404 rather than fail while rendering another person's data.
    @Test void foreignCatalogueEditIsNotFound() throws Exception {
        mvc.perform(get("/staff/applications/"+course.getCourseId()+"/edit-catalogue").session(session(peer)))
                .andExpect(status().isNotFound());
    }

    // The service accepts only the minimum claim inputs, with identity taken from the account.
    private CourseFeeApplication submit(User user,Integer reviewer) { return service.submit(course.getCourseId(),true,pdf(),pdf(),user,reviewer); }
    private MockMultipartFile pdf() { return new MockMultipartFile("receipt","proof.pdf","application/pdf","%PDF-1.4\n1 0 obj <<>> endobj\n%%EOF".getBytes()); }
    private MockHttpSession session(User user) { MockHttpSession session=new MockHttpSession(); session.setAttribute("user",user); return session; }
    private CourseApplication completed(User user) {
        CourseApplication value=new CourseApplication(); value.setApplicant(user); value.setCourseTitle("Completed course");
        value.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE); value.setCourseStartDate(LocalDate.now().minusDays(10));
        value.setCourseEndDate(value.getCourseStartDate()); value.setTrainingDays(1d); value.setCourseFee(new BigDecimal("123.45")); value.setStatus(ApplicationStatus.COMPLETED);
        return applications.saveAndFlush(value);
    }
    private <T extends User> T account(T user,String name) { user.setUserName(name); user.setStaffId(name); user.setName(name); user.setPassword("test"); return users.saveAndFlush(user); }
}

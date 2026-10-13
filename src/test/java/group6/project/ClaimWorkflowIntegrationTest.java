// We check evidence, review and reimbursement through the claim workflow.
package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    Staff employee;
    Manager boss, peer;
    Admin admin;
    CourseApplication course;
    MockMvc mvc;

    // Persist real roles and reviewer assignments, with automatic rollback after every case.
    @BeforeEach
    void prepare() {
        boss = account(new Manager(), "claim_boss");
        peer = account(new Manager(), "claim_peer");
        admin = account(new Admin(), "claim_admin");
        employee = new Staff();
        employee.setManager(boss);
        employee = account(employee, "claim_employee");
        course = completed(employee);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // Staff submit, the manager decides and Admin records the actual payment as separate events.
    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    void completeClaimFlowKeepsApprovalSeparateFromPayment(String decision) throws Exception {
        var claim = submit(employee, null);
        Long old = claim.getVersion();
        service.decide(
                claim.getApplicationId(), boss.getUserId(), decision, "Receipt checked", old);
        claims.flush();
        assertEquals(
                "approve".equals(decision)
                        ? ApplicationStatus.APPROVED
                        : ApplicationStatus.REJECTED,
                claim.getApplicationStatus());
        assertSame(boss, claim.getReviewer());
        assertNull(claim.getReimbursedAt());
        assertEquals(
                0,
                service.reimbursed(employee, LocalDate.now().getYear()).compareTo(BigDecimal.ZERO));
        if ("approve".equals(decision)) {
            assertThrows(
                    ResponseStatusException.class,
                    () ->
                            service.reimburse(
                                    claim.getApplicationId(), admin.getUserId(), "TX-1", old));
            MockHttpSession adminSession = session(admin);
            String page =
                    mvc.perform(get("/admin/payments").session(adminSession))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();
            String action = "/admin/payments/" + claim.getApplicationId();
            String paymentForm = page.substring(page.indexOf("action=\"" + action + "\""));
            paymentForm = paymentForm.substring(0, paymentForm.indexOf("</form>"));
            assertTrue(paymentForm.contains("name=\"_csrf\""));
            mvc.perform(
                            post(action)
                                    .session(adminSession)
                                    .param("_csrf", (String) adminSession.getAttribute("csrfToken"))
                                    .param("version", claim.getVersion().toString())
                                    .param("reference", "TX-1"))
                    .andExpect(status().is3xxRedirection());
            claims.flush();
            assertNotNull(claim.getReimbursedAt());
            assertSame(admin, claim.getReimbursedBy());
            assertEquals(
                    new BigDecimal("123.45"),
                    service.reimbursed(employee, LocalDate.now().getYear()));
            assertThrows(
                    ResponseStatusException.class,
                    () ->
                            service.reimburse(
                                    claim.getApplicationId(),
                                    admin.getUserId(),
                                    "TX-2",
                                    claim.getVersion()));
        } else
            assertThrows(
                    ResponseStatusException.class,
                    () ->
                            service.reimburse(
                                    claim.getApplicationId(),
                                    admin.getUserId(),
                                    "TX-1",
                                    claim.getVersion()));
        mvc.perform(get("/staff/claims/" + claim.getApplicationId()).session(session(employee)))
                .andExpect(status().isOk());
        mvc.perform(get("/manager/claims/" + claim.getApplicationId()).session(session(boss)))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/payments").session(session(admin))).andExpect(status().isOk());
    }

    // Invalid or duplicate submissions leave the claim table unchanged.
    @Test
    void eligibilityAndDuplicateChecks() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.submit(course.getCourseId(), false, pdf(), pdf(), employee, null));
        course.setStatus(ApplicationStatus.APPROVED);
        assertThrows(ResponseStatusException.class, () -> submit(employee, null));
        course.setStatus(ApplicationStatus.COMPLETED);
        course.setCourseCategory(CourseCategoryType.INTERNAL_TRAINING);
        assertThrows(ResponseStatusException.class, () -> submit(employee, null));
        course.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);
        submit(employee, null);
        assertThrows(ResponseStatusException.class, () -> submit(employee, null));
        assertEquals(1, claims.count());
    }

    // Removing format checks does not allow missing, empty or oversized evidence.
    @Test
    void invalidUploadsAreRejected() {
        for (MockMultipartFile bad :
                java.util.List.of(
                        new MockMultipartFile("receipt", "empty.txt", "text/plain", new byte[0]),
                        new MockMultipartFile(
                                "receipt",
                                "large.pdf",
                                "application/pdf",
                                new byte[5 * 1024 * 1024 + 1]))) {
            assertThrows(
                    ResponseStatusException.class,
                    () -> service.submit(course.getCourseId(), true, bad, pdf(), employee, null));
        }
        assertThrows(
                ResponseStatusException.class,
                () -> service.submit(course.getCourseId(), true, null, pdf(), employee, null));
        assertThrows(
                ResponseStatusException.class,
                () -> service.submit(course.getCourseId(), true, pdf(), null, employee, null));
        assertEquals(0, claims.count());
    }

    // Extensions, declared types and bytes need not match; downloads retain the original bytes.
    @ParameterizedTest
    @ValueSource(strings = {"receipt.docx", "receipt.heic", "receipt.txt", "receipt", "receipt.pdf"})
    void anyDocumentFormatCanBeSubmittedAndDownloaded(String filename) throws Exception {
        byte[] bytes = "Classroom receipt sample".getBytes();
        var receipt = new MockMultipartFile("receipt", filename, "image/jpeg", bytes);
        var certificate =
                new MockMultipartFile("certificate", "certificate.zip", null, new byte[] {1, 2});
        var claim =
                service.submit(course.getCourseId(), true, receipt, certificate, employee, null);
        claims.flush();
        assertEquals(filename, claim.getReceiptFileName());
        assertEquals("application/octet-stream", claim.getCertificateContentType());
        mvc.perform(
                        get("/manager/claims/" + claim.getApplicationId() + "/receipt")
                                .session(session(boss)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(
                        "Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")));
    }

    // Review reasons, current versions and actor scope all apply before a decision is saved.
    @Test
    void invalidAndRepeatedDecisionsAreRejected() {
        var claim = submit(employee, null);
        assertThrows(
                ResponseStatusException.class,
                () ->
                        service.decide(
                                claim.getApplicationId(),
                                boss.getUserId(),
                                "approve",
                                " ",
                                claim.getVersion()));
        assertThrows(
                ResponseStatusException.class,
                () ->
                        service.decide(
                                claim.getApplicationId(),
                                boss.getUserId(),
                                "approve",
                                "Good",
                                99L));
        assertThrows(
                ResponseStatusException.class,
                () ->
                        service.decide(
                                claim.getApplicationId(),
                                peer.getUserId(),
                                "approve",
                                "Good",
                                claim.getVersion()));
        service.decide(
                claim.getApplicationId(), boss.getUserId(), "approve", "Good", claim.getVersion());
        claims.flush();
        assertThrows(
                ResponseStatusException.class,
                () ->
                        service.decide(
                                claim.getApplicationId(),
                                boss.getUserId(),
                                "reject",
                                "Other",
                                claim.getVersion()));
    }

    // Evidence is limited to its applicant, assigned reviewer and Admin.
    @Test
    void documentPrivacyAndQueueRendering() throws Exception {
        var claim = submit(employee, null);
        String path = "/manager/claims/" + claim.getApplicationId();
        mvc.perform(get("/manager/claims").session(session(boss))).andExpect(status().isOk());
        mvc.perform(get(path + "/receipt").session(session(boss)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdf().getBytes()));
        mvc.perform(get(path + "/receipt").session(session(peer))).andExpect(status().isNotFound());
        mvc.perform(get(path + "/receipt")).andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/staff/claims/" + claim.getApplicationId() + "/receipt")
                                .session(session(peer)))
                .andExpect(status().isNotFound());
    }

    // An unassigned Manager chooses another Manager for personal courses and claims, never
    // themselves.
    @Test
    void peerReviewWorksWithoutReportingCycles() throws Exception {
        applications.delete(course);
        applications.flush();
        course = completed(boss);
        assertThrows(ResponseStatusException.class, () -> submit(boss, boss.getUserId()));
        var claim = submit(boss, peer.getUserId());
        assertThrows(
                ResponseStatusException.class,
                () ->
                        service.decide(
                                claim.getApplicationId(),
                                boss.getUserId(),
                                "approve",
                                "Self",
                                claim.getVersion()));
        service.decide(
                claim.getApplicationId(),
                peer.getUserId(),
                "approve",
                "Peer review",
                claim.getVersion());
        assertNull(boss.getManager());
        mvc.perform(get("/staff/fee").session(session(boss))).andExpect(status().isOk());
    }

    // Existing requests keep their assigned reviewer when Admin changes the reporting relationship.
    @Test
    void reportingChangesDoNotMovePendingClaims() {
        var claim = submit(employee, null);
        employee.setManager(peer);
        users.saveAndFlush(employee);
        assertEquals(boss.getUserId(), claim.getApprovalManager().getUserId());
        assertEquals(1, service.pending(boss.getUserId(), 0, 10).getTotalElements());
        assertTrue(service.pending(peer.getUserId(), 0, 10).isEmpty());
    }

    // An incomplete claim can be rejected with a reason, but cannot be approved or paid.
    @Test
    void incompleteClaimCanBeRejected() {
        var claim = submit(employee, null);
        claim.setCertificate(null);
        claims.saveAndFlush(claim);
        assertThrows(
                ResponseStatusException.class,
                () ->
                        service.decide(
                                claim.getApplicationId(),
                                boss.getUserId(),
                                "approve",
                                "Checked",
                                claim.getVersion()));
        service.decide(
                claim.getApplicationId(),
                boss.getUserId(),
                "reject",
                "Missing completion evidence",
                claim.getVersion());
        assertEquals(ApplicationStatus.REJECTED, claim.getApplicationStatus());
    }

    // Assigned peer reviews also protect a manager from being demoted before the queue is resolved.
    @Test
    void pendingPeerReviewPreventsRemovingManagerAccess() {
        applications.delete(course);
        applications.flush();
        course = completed(boss);
        submit(boss, peer.getUserId());
        var form = accounts.form(peer);
        form.setRole(Roles.STAFF);
        assertThrows(
                ResponseStatusException.class,
                () -> accounts.save(peer.getUserId(), form, admin.getUserId()));
        assertInstanceOf(Manager.class, users.findById(peer.getUserId()).orElseThrow());
    }

    // Foreign catalogue-edit URLs must return 404 rather than fail while rendering another person's
    // data.
    @Test
    void foreignCatalogueEditIsNotFound() throws Exception {
        mvc.perform(
                        get("/staff/applications/" + course.getCourseId() + "/edit")
                                .session(session(peer)))
                .andExpect(status().isNotFound());
    }

    // All claim workspaces page metadata consistently, including approved payment records.
    @Test
    void claimListsUseFiveByDefaultAndSupportTenAndFifteen() throws Exception {
        var submitted = new java.util.ArrayList<CourseFeeApplication>();
        for (int i = 0; i < 16; i++) {
            if (i > 0) course = completed(employee);
            submitted.add(submit(employee, null));
        }
        assertClaimPages("/staff/fee", employee);
        assertClaimPages("/manager/claims", boss);
        for (var claim : submitted)
            service.decide(
                    claim.getApplicationId(),
                    boss.getUserId(),
                    "approve",
                    "Receipt checked",
                    claim.getVersion());
        claims.flush();
        assertClaimPages("/admin/payments", admin);
        var invalid =
                mvc.perform(TestRequests.post("/staff/fee").session(session(employee)))
                        .andExpect(status().isBadRequest())
                        .andReturn();
        var page = (Page<?>) invalid.getModelAndView().getModel().get("claimPage");
        assertEquals(5, page.getSize());
    }

    private void assertClaimPages(String route, User user) throws Exception {
        var first =
                mvc.perform(get(route).session(session(user)))
                        .andExpect(status().isOk())
                        .andExpect(model().attribute("claims", org.hamcrest.Matchers.hasSize(5)))
                        .andReturn();
        var firstPage = (Page<?>) first.getModelAndView().getModel().get("claimPage");
        assertEquals(5, firstPage.getSize());
        assertEquals(16, firstPage.getTotalElements());
        for (int size : new int[] {5, 10, 15, 20}) {
            int expectedSize = size == 20 ? 5 : size;
            var result =
                    mvc.perform(
                                    get(route)
                                            .session(session(user))
                                            .param("page", "1")
                                            .param("size", String.valueOf(size)))
                            .andExpect(status().isOk())
                            .andExpect(
                                    model().attribute(
                                                    "claims",
                                                    org.hamcrest.Matchers.hasSize(
                                                            Math.min(
                                                                    expectedSize,
                                                                    16 - expectedSize))))
                            .andReturn();
            var page = (Page<?>) result.getModelAndView().getModel().get("claimPage");
            assertEquals(expectedSize, page.getSize());
            assertEquals(1, page.getNumber());
            assertEquals(16, page.getTotalElements());
        }
    }

    // The service accepts only the minimum claim inputs, with identity taken from the account.
    private CourseFeeApplication submit(User user, Integer reviewer) {
        return service.submit(course.getCourseId(), true, pdf(), pdf(), user, reviewer);
    }

    // Use a small matching PDF fixture for the upload checks.
    private MockMultipartFile pdf() {
        return new MockMultipartFile(
                "receipt",
                "proof.pdf",
                "application/pdf",
                "%PDF-1.4\n1 0 obj <<>> endobj\n%%EOF".getBytes());
    }

    // Create the shared authenticated session contract used by MVC pages.
    private MockHttpSession session(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }

    // Persist a finished paid course that is eligible for a fee claim.
    private CourseApplication completed(User user) {
        CourseApplication value = new CourseApplication();
        value.setApplicant(user);
        value.setCourseTitle("Completed course");
        value.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);
        value.setCourseStartDate(LocalDate.now().minusDays(10));
        value.setCourseEndDate(value.getCourseStartDate());
        value.setTrainingDays(1d);
        value.setCourseFee(new BigDecimal("123.45"));
        value.setStatus(ApplicationStatus.COMPLETED);
        return applications.saveAndFlush(value);
    }

    // Create a distinct persisted account without depending on development seeds.
    private <T extends User> T account(T user, String name) {
        user.setUserName(name);
        user.setStaffId(name);
        user.setName(name);
        user.setPassword("test");
        return users.saveAndFlush(user);
    }
}

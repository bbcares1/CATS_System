package group6.project;

import static org.junit.jupiter.api.Assertions.*;

import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest
class ClaimConcurrencyIntegrationTest {
    @Autowired UserRepo users;
    @Autowired CourseApplicationRepo applications;
    @Autowired CourseFeeApplicationRepo claims;
    @Autowired TrainingEntitlementRepo entitlements;
    @Autowired TrainingEntitlementService allowances;
    @Autowired CourseFeeApplicationService service;
    @Autowired CourseApplicationService policy;
    Manager one, two;
    CourseApplication completed;
    LocalDate date;

    // Committed fixtures and separate worker transactions reproduce real double clicks and peer
    // requests.
    @BeforeEach
    void prepare() {
        one = manager();
        two = manager();
        date = LocalDate.now().plusDays(7);
        while (date.getDayOfWeek().getValue() > 5) date = date.plusDays(1);
        for (Manager m : List.of(one, two))
            allowances.saveLimits(m.getUserId(), date.getYear(), 10, new BigDecimal("2000"));
        completed = form(two);
        completed.setApplicant(one);
        completed.setStatus(ApplicationStatus.COMPLETED);
        completed.setCourseStartDate(LocalDate.now().minusDays(10));
        completed.setCourseEndDate(completed.getCourseStartDate());
        completed = applications.saveAndFlush(completed);
    }

    // Only one parallel claim can be stored for a completed course.
    @Test
    void duplicateClaimsAreSerialized() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Boolean> task =
                    () -> {
                        start.await();
                        try {
                            service.submit(
                                    completed.getCourseId(),
                                    true,
                                    pdf(),
                                    pdf(),
                                    one,
                                    two.getUserId());
                            return true;
                        } catch (IllegalArgumentException e) {
                            assertTrue(e.getMessage().contains("already exists"));
                            return false;
                        }
                    };
            Future<Boolean> first = workers.submit(task), second = workers.submit(task);
            start.countDown();
            assertNotEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertEquals(1, claims.findByApplicant_UserId(one.getUserId()).size());
        } finally {
            workers.shutdownNow();
        }
    }

    // Reciprocal peer requests lock the same two accounts in the same order and both complete.
    @Test
    void simultaneousPeerRequestsDoNotDeadlock() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<CourseApplication> first =
                    workers.submit(
                            () -> {
                                start.await();
                                return policy.create(form(two), one);
                            });
            Future<CourseApplication> second =
                    workers.submit(
                            () -> {
                                start.await();
                                return policy.create(form(one), two);
                            });
            start.countDown();
            assertEquals(
                    two.getUserId(),
                    first.get(20, TimeUnit.SECONDS).getApprovalManager().getUserId());
            assertEquals(
                    one.getUserId(),
                    second.get(20, TimeUnit.SECONDS).getApprovalManager().getUserId());
        } finally {
            workers.shutdownNow();
        }
    }

    // Delete only these test records in dependency order, never development data.
    @AfterEach
    void cleanup() {
        for (Manager m : List.of(one, two))
            claims.deleteAll(claims.findByApplicant_UserId(m.getUserId()));
        for (Manager m : List.of(one, two)) {
            applications.deleteAll(
                    applications.findByApplicant_UserIdAndStatusIn(
                            m.getUserId(), List.of(ApplicationStatus.values())));
            entitlements.deleteAll(entitlements.findByStaff_UserId(m.getUserId()));
        }
        users.deleteAll(List.of(one, two));
    }

    // Each worker receives its own request, including an explicit peer reviewer.
    private CourseApplication form(Manager peer) {
        CourseApplication c = new CourseApplication();
        c.setCourseTitle("Peer concurrency");
        c.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);
        c.setTrainingProvider("ISS");
        c.setJustification("Develop skills");
        c.setCourseFee(new BigDecimal("100"));
        c.setCourseStartDate(date);
        c.setCourseEndDate(date);
        c.setApprovalManagerId(peer.getUserId());
        return c;
    }

    // Create a Manager identity for the reviewer or role-scope fixture.
    private Manager manager() {
        Manager m = new Manager();
        String key = UUID.randomUUID().toString();
        m.setUserName(key);
        m.setStaffId(key);
        m.setName(key);
        m.setPassword("test");
        return users.saveAndFlush(m);
    }

    // Use a small matching PDF fixture for the upload checks.
    private MockMultipartFile pdf() {
        return new MockMultipartFile(
                "receipt", "proof.pdf", "application/pdf", "%PDF-1.4\n%%EOF".getBytes());
    }
}

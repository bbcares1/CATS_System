package group6.project;

import group6.project.form.*;
import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApplicationConcurrencyTest {
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired UserRepo users;
    @Autowired CourseApplicationRepo applications;
    @Autowired TrainingEntitlementRepo entitlements;
    @Autowired CourseApplicationService service;
    @Autowired TrainingEntitlementService allowances;
    @Autowired CourseProviderRepo providers;
    @Autowired CourseCategoryRepository categories;
    @Autowired CourseDetailRepo courses;
    @Autowired CourseBatchRepo batches;
    private final List<User> accounts = new ArrayList<>();
    private Manager manager;
    private Manager peer;
    private Staff first;
    private Staff second;
    private CourseProvider provider;
    private CourseDetail course;
    private CourseBatch batch;
    private final LocalDate date = LocalDate.now().plusYears(1).withDayOfYear(1)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    // Fixtures are committed so two independent request transactions can see them.
    @BeforeEach
    void setup() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            manager = account(new Manager(), "manager"); peer = account(new Manager(), "peer");
            first = new Staff(); first.setManager(manager); first = account(first, "first");
            second = new Staff(); second.setManager(manager); second = account(second, "second");
            for (User user : accounts) allowances.saveLimits(user.getUserId(), date.getYear(), 1, new BigDecimal("100"));
            provider = new CourseProvider(); provider.setName("Concurrent " + UUID.randomUUID()); provider = providers.save(provider);
            course = new CourseDetail(); course.setTitle("One place"); course.setCourseFee(new BigDecimal("100"));
            course.setProvider(provider); course.setCourseCategory(categories.findById(2).orElseThrow()); course.setActive(true);
            course = courses.save(course);
            batch = new CourseBatch(); batch.setCourseDetail(course); batch.setCourseStartDate(date); batch.setCourseEndDate(date);
            batch.setTrainingDays(1d); batch.setCapacity(1); batch.setActive(true); batch = batches.save(batch);
        });
    }

    // Remove only records created by this test, leaving shared reference categories alone.
    @AfterEach
    void cleanup() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            for (User user : accounts) {
                applications.deleteAll(applications.findByApplicant_UserIdAndCourseStartDateBetweenOrderByCourseStartDateAsc(
                        user.getUserId(), date.withDayOfYear(1), date.withMonth(12).withDayOfMonth(31)));
                entitlements.findByStaff_UserIdAndYear(user.getUserId(), date.getYear()).ifPresent(entitlements::delete);
            }
            applications.flush();
            if (batch != null) batches.deleteById(batch.getBatchId());
            if (course != null) courses.deleteById(course.getCourseId());
            if (provider != null) providers.deleteById(provider.getProviderId());
            for (User user : accounts.reversed()) users.deleteById(user.getUserId());
        });
    }

    @Test
    void twoSubmissionsCannotSpendTheSameAllowance() throws Exception {
        var results = race(() -> service.createOther(form(date), first),
                () -> service.createOther(form(date.plusDays(1)), first));
        assertEquals(List.of(200, 400), results);
        assertEquals(1, allowances.summary(first, date.getYear(), null).reservedDays());
    }

    @Test
    void twoEmployeesCannotReserveTheLastSeat() throws Exception {
        var results = race(() -> service.createFromCatalogue(course.getCourseId(), catalogue(), first),
                () -> service.createFromCatalogue(course.getCourseId(), catalogue(), second));
        assertEquals(List.of(200, 400), results);
        assertEquals(1, applications.countByCatalogueBatch_BatchIdAndStatusIn(batch.getBatchId(), List.of(ApplicationStatus.APPLIED)));
    }

    @Test
    void simultaneousDecisionsDoNotOverwriteEachOther() throws Exception {
        var saved = service.createOther(form(date), first);
        var results = race(() -> service.decide(saved.getCourseId(), decision(saved, true), manager),
                () -> service.decide(saved.getCourseId(), decision(saved, false), manager));
        assertEquals(List.of(200, 409), results);
        assertEquals(1L, applications.findById(saved.getCourseId()).orElseThrow().getVersion());
    }

    @Test
    void managersCanApplyToEachOtherWithoutAReverseLockOrder() throws Exception {
        var firstForm = form(date); firstForm.setReviewerId(peer.getUserId());
        var secondForm = form(date); secondForm.setReviewerId(manager.getUserId());
        assertEquals(List.of(200, 200), race(() -> service.createOther(firstForm, manager),
                () -> service.createOther(secondForm, peer)));
    }

    private <T extends User> T account(T user, String name) {
        user.setUserName("race-" + name + "-" + UUID.randomUUID()); user.setName(name); user.setPassword("demo123");
        T saved = users.save(user); accounts.add(saved); return saved;
    }

    private CourseApplicationForm form(LocalDate day) {
        var form = new CourseApplicationForm(); form.setCourseTitle("Concurrent course"); form.setTrainingProvider("Example");
        form.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE); form.setCourseFee(new BigDecimal("100"));
        form.setCourseStartDate(day); form.setCourseEndDate(day); form.setJustification("Practice Java"); return form;
    }

    private CatalogueApplicationForm catalogue() {
        var form = new CatalogueApplicationForm(); form.setCourseVersion(course.getVersion());
        form.setBatchId(batch.getBatchId()); form.setBatchVersion(batch.getVersion()); form.setJustification("Practice Java"); return form;
    }

    private DecisionForm decision(CourseApplication application, boolean approved) {
        var form = new DecisionForm(); form.setVersion(application.getVersion()); form.setApproved(approved);
        form.setReason("Checked the request"); return form;
    }

    // Start both requests together; unexpected exceptions or a timeout fail the test.
    private List<Integer> race(Supplier<?> firstAction, Supplier<?> secondAction) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier ready = new CyclicBarrier(2);
        try {
            Callable<Integer> one = () -> result(ready, firstAction);
            Callable<Integer> two = () -> result(ready, secondAction);
            Future<Integer> firstResult = pool.submit(one);
            Future<Integer> secondResult = pool.submit(two);
            return java.util.stream.Stream.of(firstResult.get(20, TimeUnit.SECONDS), secondResult.get(20, TimeUnit.SECONDS)).sorted().toList();
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private int result(CyclicBarrier ready, Supplier<?> action) throws Exception {
        ready.await(5, TimeUnit.SECONDS);
        try { action.get(); return 200; }
        catch (ResponseStatusException error) { return error.getStatusCode().value(); }
    }
}

package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import group6.project.model.*;
import group6.project.model.form.CatalogueApplicationForm;
import group6.project.repo.*;
import group6.project.service.*;

@SpringBootTest
class CatalogueSeatConcurrencyIntegrationTest {
    @Autowired StaffRepo employees;
    @Autowired CourseDetailRepo courses;
    @Autowired CourseBatchRepo batches;
    @Autowired CourseCategoryRepository categories;
    @Autowired CourseApplicationRepo applications;
    @Autowired TrainingEntitlementRepo entitlements;
    @Autowired TrainingEntitlementService allowances;
    @Autowired CourseCatalogueService catalogue;
    Staff first, second; CourseDetail course; CourseBatch batch; LocalDate day;

    // Separate employees and committed fixtures reproduce competition for one remaining seat.
    @BeforeEach
    void prepare() {
        day = LocalDate.now().plusDays(7); while (day.getDayOfWeek().getValue() > 5) day = day.plusDays(1);
        first = employee(); second = employee();
        course = new CourseDetail(); course.setTitle("Last seat fixture"); course.setTrainingProvider("Training centre");
        course.setCourseFee(new BigDecimal("10.00")); course.setCourseCategory(categories.findAll().stream()
                .filter(c -> c.getKind() == CourseCategoryType.EXTERNAL_COURSE).findFirst().orElseThrow()); course = courses.saveAndFlush(course);
        batch = new CourseBatch(); batch.setCourseDetail(course); batch.setCourseStartDate(day); batch.setCourseEndDate(day);
        batch.setCapacity(1); batch = batches.saveAndFlush(batch);
    }

    // A waiting transaction must count the first committed reservation before accepting another.
    @Test
    void concurrentEmployeesCannotOverbookTheLastSeat() throws Exception {
        CountDownLatch start = new CountDownLatch(1); ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> one = workers.submit(() -> submit(start, first)); Future<Boolean> two = workers.submit(() -> submit(start, second));
            start.countDown(); assertNotEquals(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
            assertEquals(1, applications.countByCatalogueBatch_BatchIdAndStatusIn(batch.getBatchId(), CourseApplicationService.RESERVED_STATUSES));
        } finally { workers.shutdownNow(); }
    }

    // Each request creates its own form and joins only after both workers are ready.
    private boolean submit(CountDownLatch start, Staff employee) throws Exception {
        start.await(); CatalogueApplicationForm form = new CatalogueApplicationForm(); form.setCourseVersion(course.getVersion());
        form.setScheduledBatch(batch.getBatchId() + ":" + batch.getVersion()); form.setJustification("Develop skills");
        try { catalogue.submit(course.getCourseId(), form, employee); return true; }
        catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("places")); return false; }
    }

    // Each employee has enough allowance; only the seat count should decide this test.
    private Staff employee() {
        Staff employee = new Staff(); String key = UUID.randomUUID().toString(); employee.setUserName(key); employee.setStaffId(key);
        employee.setName("Seat fixture"); employee.setPassword("test"); employee.setRole(Roles.STAFF); employee = employees.saveAndFlush(employee);
        allowances.saveLimits(employee.getUserId(), day.getYear(), 10, new BigDecimal("1000.00")); return employee;
    }

    // Remove only these fixtures, in foreign-key order, from the isolated test database.
    @AfterEach
    void cleanup() {
        for (Staff employee : List.of(first, second)) {
            applications.deleteAll(applications.findByApplicant_UserIdAndStatusIn(employee.getUserId(), List.of(ApplicationStatus.values())));
            entitlements.deleteAll(entitlements.findByStaff_UserId(employee.getUserId())); employees.deleteById(employee.getUserId());
        }
        batches.deleteById(batch.getBatchId()); courses.deleteById(course.getCourseId());
    }
}

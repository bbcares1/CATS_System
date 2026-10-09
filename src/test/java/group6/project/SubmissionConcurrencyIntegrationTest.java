package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.*;

@SpringBootTest
class SubmissionConcurrencyIntegrationTest {
    @Autowired StaffRepo employees;
    @Autowired CourseApplicationRepo applications;
    @Autowired TrainingEntitlementRepo entitlements;
    @Autowired CourseApplicationService policy;
    @Autowired TrainingEntitlementService allowances;
    Staff staff;
    LocalDate date;

    // Separate transactions are needed to reproduce two simultaneous browser submissions.
    @BeforeEach
    void prepare() {
        staff=new Staff();
        String key=UUID.randomUUID().toString();
        staff.setUserName(key); staff.setStaffId(key); staff.setName("Concurrent fixture");
        staff.setPassword("test"); staff.setRole(Roles.STAFF);
        staff=employees.saveAndFlush(staff);
        date=LocalDate.now().plusDays(7);
        while(date.getDayOfWeek().getValue()>5) date=date.plusDays(1);
        allowances.saveLimits(staff.getUserId(),date.getYear(),1,new BigDecimal("100.00"));
    }

    // Only one request can reserve the last day and budget, even if both arrive together.
    @Test
    void simultaneousSubmissionsCannotOverspendTheAnnualAllowance() throws Exception {
        CountDownLatch start=new CountDownLatch(1);
        ExecutorService workers=Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> submit=()->{
                start.await();
                try { policy.create(form(),staff); return true; }
                catch(IllegalArgumentException e) { return false; }
            };
            Future<Boolean> first=workers.submit(submit);
            Future<Boolean> second=workers.submit(submit);
            start.countDown();
            assertNotEquals(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS));
            assertEquals(1,policy.findForStaffAndYear(staff,date.getYear()).size());
            assertEquals(1d,policy.summaryForYear(staff,date.getYear(),null).usedDays());
        } finally { workers.shutdownNow(); }
    }

    // Remove only this test's records; it never uses development accounts.
    @AfterEach
    void cleanUp() {
        applications.deleteAll(policy.findForStaffAndYear(staff,date.getYear()));
        entitlements.deleteAll(entitlements.findByStaff_UserId(staff.getUserId()));
        employees.deleteById(staff.getUserId());
    }

    // Every worker receives its own form so the requests share only employee identity.
    private CourseApplication form() {
        CourseApplication course=new CourseApplication();
        course.setCourseTitle("Concurrent course"); course.setTrainingProvider("Training centre");
        course.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE); course.setJustification("Improve skills");
        course.setCourseStartDate(date); course.setCourseEndDate(date); course.setCourseFee(new BigDecimal("100.00"));
        return course;
    }
}

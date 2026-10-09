package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import group6.project.model.*;
import group6.project.model.form.HolidayForm;
import group6.project.repo.*;
import group6.project.service.*;

@SpringBootTest
class CalendarConcurrencyIntegrationTest {
    @Autowired UserRepo users;
    @Autowired CourseApplicationRepo applications;
    @Autowired TrainingEntitlementRepo entitlements;
    @Autowired TrainingEntitlementService allowances;
    @Autowired CourseApplicationService policy;
    @Autowired ExcludedDaysService holidays;
    @Autowired ExcludedDaysRepo holidayRepo;
    @Autowired TrainingCalendarPolicyRepo calendar;
    @Autowired PlatformTransactionManager transactions;
    Manager manager;Staff employee;LocalDate date;

    // Use committed fixtures and independent transactions so the calendar lock is exercised by MySQL too.
    @BeforeEach void prepare() {
        manager=account(new Manager());employee=new Staff();employee.setManager(manager);employee=account(employee);
        date=LocalDate.now().plusDays(50);while(date.getDayOfWeek().getValue()>5)date=date.plusDays(1);
        allowances.saveLimits(employee.getUserId(),date.getYear(),10,new BigDecimal("2000"));
    }

    // If submission owns the calendar first, the holiday edit waits then rejects the affected active course.
    @Test void holidayCannotAppearBetweenValidationAndSave() throws Exception {
        ExecutorService workers=Executors.newFixedThreadPool(2);CountDownLatch locked=new CountDownLatch(1),release=new CountDownLatch(1);
        try {
            Future<CourseApplication> submit=workers.submit(()->new TransactionTemplate(transactions).execute(tx->{
                policy.prepareReviewer(employee,null);calendar.readCalendar();locked.countDown();await(release);return policy.create(form(),employee);
            }));assertTrue(locked.await(5,TimeUnit.SECONDS));
            Future<Boolean> edit=workers.submit(()->{try{holidays.save(null,holiday());return true;}catch(IllegalArgumentException e){return false;}});
            assertThrows(TimeoutException.class,()->edit.get(150,TimeUnit.MILLISECONDS));release.countDown();
            assertNotNull(submit.get(10,TimeUnit.SECONDS));assertFalse(edit.get(10,TimeUnit.SECONDS));assertFalse(holidayRepo.existsByDate(date));
        } finally {release.countDown();workers.shutdownNow();}
    }

    // If the holiday edit owns the calendar first, submission sees the committed holiday and is rejected.
    @Test void submissionSeesAHolidayCommittedWhileItWaits() throws Exception {
        ExecutorService workers=Executors.newFixedThreadPool(2);CountDownLatch locked=new CountDownLatch(1),release=new CountDownLatch(1);
        try {
            Future<?> edit=workers.submit(()->new TransactionTemplate(transactions).execute(tx->{calendar.editCalendar();locked.countDown();await(release);holidays.save(null,holiday());return null;}));
            assertTrue(locked.await(5,TimeUnit.SECONDS));
            Future<Boolean> submit=workers.submit(()->{try{policy.create(form(),employee);return true;}catch(IllegalArgumentException e){return false;}});
            assertThrows(TimeoutException.class,()->submit.get(150,TimeUnit.MILLISECONDS));release.countDown();edit.get(10,TimeUnit.SECONDS);
            assertFalse(submit.get(10,TimeUnit.SECONDS));assertTrue(holidayRepo.existsByDate(date));
        } finally {release.countDown();workers.shutdownNow();}
    }

    // Remove only this case's fixtures; shared test categories and calendar policy remain intact.
    @AfterEach void cleanup() {
        applications.deleteAll(applications.findByApplicant_UserIdAndStatusIn(employee.getUserId(),List.of(ApplicationStatus.values())));
        entitlements.deleteAll(entitlements.findByStaff_UserId(employee.getUserId()));
        holidayRepo.deleteAll(holidayRepo.findAll().stream().filter(h->h.getDate().equals(date)&&h.getDescription().equals("Concurrent calendar fixture")).toList());
        users.delete(employee);users.delete(manager);
    }

    // Both workers use the same date while submission supplies valid ordinary application fields.
    private CourseApplication form() {var c=new CourseApplication();c.setCourseTitle("Concurrent calendar fixture");c.setTrainingProvider("ISS");c.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);c.setCourseStartDate(date);c.setCourseEndDate(date);c.setJustification("Develop skills");return c;}
    private HolidayForm holiday() {var form=new HolidayForm();form.setDate(date);form.setDescription("Concurrent calendar fixture");return form;}
    private <T extends User>T account(T user) {String key=UUID.randomUUID().toString();user.setUserName(key);user.setStaffId(key);user.setName(key);user.setPassword("test");return users.saveAndFlush(user);}
    private void await(CountDownLatch latch) {try{if(!latch.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Test barrier timed out.");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
}

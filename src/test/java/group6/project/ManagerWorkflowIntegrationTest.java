package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.*;

@SpringBootTest
@Transactional
class ManagerWorkflowIntegrationTest {
    @Autowired StaffRepo employees;
    @Autowired CourseApplicationRepo applications;
    @Autowired CourseApplicationService policy;
    @Autowired TrainingEntitlementService allowances;
    @Autowired ManagerService managers;
    Manager bob, chris;
    Staff alex, sam, pat;
    LocalDate date;

    // Real JOINED accounts and manager assignments exercise the repository access rules.
    @BeforeEach
    void prepare() {
        date=LocalDate.now().plusDays(7);
        while(date.getDayOfWeek().getValue()>5) date=date.plusDays(1);
        bob=account(new Manager(),"bob_review",null);
        chris=account(new Manager(),"chris_review",null);
        alex=account(new Staff(),"alex_review",bob);
        sam=account(new Staff(),"sam_review",bob);
        pat=account(new Staff(),"pat_review",chris);
    }

    // Decisions persist reviewer/reason and keep the Staff view and annual totals consistent.
    @ParameterizedTest
    @ValueSource(strings={"approve","reject"})
    void staffSubmissionAndManagerDecisionShareTheSameRecord(String decision) {
        CourseApplication course=policy.create(form(),alex);
        managers.decide(bob.getUserId(),course.getCourseId(),decision,"  Relevant training.  ",course.getVersion());
        applications.flush();
        var detail=managers.getApplicationForManager(bob.getUserId(),course.getCourseId());
        assertEquals("bob_review",detail.reviewerName());
        assertEquals("Relevant training.",detail.decisionReason());
        assertNotNull(detail.reviewedAt());
        assertEquals(detail.status(),policy.getOwned(course.getCourseId(),alex).getStatus());
        assertEquals("approve".equals(decision)?1d:0d,policy.summaryForYear(alex,date.getYear(),null).usedDays());
        if("approve".equals(decision)) {
            policy.cancel(course.getCourseId(),alex);
            assertEquals(0d,policy.summaryForYear(alex,date.getYear(),null).usedDays());
            assertSame(bob,course.getReviewer());
        }
    }

    // Editing after a review page was opened invalidates that page's decision version.
    @Test
    void editedApplicationCannotBeApprovedFromAnOldPage() {
        CourseApplication course=policy.create(form(),alex);
        Long oldVersion=course.getVersion();
        CourseApplication edit=form(); edit.setCourseTitle("Updated course");
        policy.update(course.getCourseId(),edit,alex); applications.flush();
        var error=assertThrows(ResponseStatusException.class,
                ()->managers.decide(bob.getUserId(),course.getCourseId(),"approve","Useful",oldVersion));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
        assertEquals(ApplicationStatus.UPDATED,course.getStatus());
        assertNull(course.getReviewer());
    }

    // Both reading and deciding are restricted to current direct reports.
    @Test
    void anotherManagerCannotReadOrDecideTheApplication() {
        CourseApplication course=policy.create(form(),alex);
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->managers.getApplicationForManager(chris.getUserId(),course.getCourseId())).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->managers.decide(chris.getUserId(),course.getCourseId(),"approve","Useful",course.getVersion())).getStatusCode());
        assertEquals(ApplicationStatus.APPLIED,course.getStatus());
    }

    // Staffing information includes only other direct reports' approved overlapping courses.
    @Test
    void supportIncludesAnnualUsageAndOnlyTheManagersOtherApprovedCourses() {
        CourseApplication request=policy.create(form(),alex);
        for(Staff employee:java.util.List.of(sam,pat,bob)) {
            CourseApplication input=form(); if(employee==bob) input.setApprovalManagerId(chris.getUserId());
            CourseApplication approved=policy.create(input,employee);
            approved.setStatus(ApplicationStatus.APPROVED); applications.saveAndFlush(approved);
        }
        var support=managers.getDecisionSupport(bob.getUserId(),request.getCourseId());
        assertEquals(1,support.overlapping().size());
        assertEquals(sam.getUserId(),support.overlapping().getFirst().applicantId());
        assertEquals(1d,support.requestAllowance().usedDays());
        assertEquals(0d,support.requestAllowance().committedDays());
    }

    // History includes all states from this year and rejects foreign employees.
    @Test
    void historyUsesTheCurrentYearAndDirectReportSelection() {
        CourseApplication current=form(); current.setApplicant(alex);
        current.setCourseStartDate(LocalDate.now()); current.setCourseEndDate(LocalDate.now());
        current.setCourseTitle("This year"); current.setStatus(ApplicationStatus.COMPLETED);
        applications.saveAndFlush(current);
        CourseApplication previous=form(); previous.setApplicant(alex);
        previous.setCourseStartDate(LocalDate.now().minusYears(1)); previous.setCourseEndDate(previous.getCourseStartDate());
        previous.setStatus(ApplicationStatus.REJECTED); applications.saveAndFlush(previous);
        var history=managers.getEmployeeHistory(bob.getUserId(),alex.getUserId());
        assertEquals(1,history.size()); assertEquals("This year",history.getFirst().title());
        assertEquals(2,managers.getSubordinates(bob.getUserId()).size());
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->managers.getEmployeeHistory(bob.getUserId(),pat.getUserId())).getStatusCode());
    }

    // Each fixture has a proper annual allocation rather than a shared Staff default.
    private <T extends Staff> T account(T employee,String name,Manager manager) {
        employee.setUserName(name); employee.setPassword("test"); employee.setName(name); employee.setStaffId(name);
        employee.setRole(employee instanceof Manager?Roles.MANAGER:Roles.STAFF); employee.setManager(manager);
        employee=employees.saveAndFlush(employee);
        allowances.saveLimits(employee.getUserId(),date.getYear(),10,new BigDecimal("3000.00"));
        return employee;
    }

    // The employee supplies course details, while identity/review/state remain server-owned.
    private CourseApplication form() {
        CourseApplication course=new CourseApplication();
        course.setCourseTitle("Manager workflow course"); course.setTrainingProvider("Training centre");
        course.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE); course.setJustification("Improve skills");
        course.setCourseStartDate(date); course.setCourseEndDate(date); course.setCourseFee(new BigDecimal("100.00"));
        return course;
    }
}

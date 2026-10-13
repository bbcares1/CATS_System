// We check workflow email content and delivery failures without sending real email.
package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import group6.project.form.AdminEmailForm;
import group6.project.model.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;

class NotificationServiceTest {
    // Capture mail locally; this test never connects to a mail server.
    @Test
    void submissionAndBothDecisionsContainRecipientReasonAndLoginLink() {
        AdminEmailService mail = mock(AdminEmailService.class);
        NotificationService service = new NotificationService(mail, "https://cats.example.test/");
        Staff staff = new Staff();
        staff.setName("Sam");
        staff.setEmail("sam@example.test");
        Manager manager = new Manager();
        manager.setEmail("manager@example.test");
        CourseApplication course = new CourseApplication();
        course.setApplicant(staff);
        course.setApprovalManager(manager);
        course.setCourseTitle("Java");
        course.setJustification("Learn Java");
        assertTrue(service.submitted(course));
        for (ApplicationStatus status :
                new ApplicationStatus[] {ApplicationStatus.APPROVED, ApplicationStatus.REJECTED}) {
            course.setStatus(status);
            course.setDecisionReason("Team training plan");
            assertTrue(service.decided(course));
        }
        var sent = ArgumentCaptor.forClass(AdminEmailForm.class);
        verify(mail, times(3)).send(sent.capture());
        assertEquals("manager@example.test", sent.getAllValues().getFirst().getRecipientEmail());
        for (AdminEmailForm form : sent.getAllValues())
            assertTrue(form.getBody().contains("https://cats.example.test/employee/login"));
        assertEquals("sam@example.test", sent.getAllValues().getLast().getRecipientEmail());
        assertTrue(sent.getAllValues().getLast().getBody().contains("Team training plan"));
    }

    // Failed delivery is reported to the caller instead of undoing a committed decision.
    @Test
    void unavailableSmtpReturnsFailure() {
        AdminEmailService mail = mock(AdminEmailService.class);
        doThrow(new MailSendException("Offline")).when(mail).send(any());
        NotificationService service = new NotificationService(mail, "http://localhost:8081");
        Staff staff = new Staff();
        staff.setEmail("sam@example.test");
        CourseApplication course = new CourseApplication();
        course.setApplicant(staff);
        course.setStatus(ApplicationStatus.REJECTED);
        assertFalse(service.decided(course));
    }
}

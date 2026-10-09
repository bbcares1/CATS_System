package group6.project.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import group6.project.model.AdminEmailForm;

class AdminEmailServiceTest {

    @Test
    void sendsConfiguredMessageToSubmittedRecipient() {
        JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
        AdminEmailService service = new AdminEmailService(mailSender, "admin@qq.com");
        AdminEmailForm form = new AdminEmailForm();
        form.setRecipientEmail("  recipient@example.com ");
        form.setSubject("  Course update  ");
        form.setBody("Please review the schedule.");

        service.send(form);

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("admin@qq.com", sent.getFrom());
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new String[] {"recipient@example.com"}, sent.getTo());
        org.junit.jupiter.api.Assertions.assertEquals("Course update", sent.getSubject());
        org.junit.jupiter.api.Assertions.assertEquals(
                "Please review the schedule.", sent.getText());
    }

    @Test
    void rejectsSendingWhenSenderAddressIsNotConfigured() {
        JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
        AdminEmailService service = new AdminEmailService(mailSender, " ");

        assertThrows(IllegalStateException.class, () -> service.send(new AdminEmailForm()));
        verifyNoInteractions(mailSender);
    }
}

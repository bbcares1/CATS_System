package group6.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import group6.project.model.AdminEmailForm;

@Service
public class AdminEmailService {

    private final JavaMailSender mailSender;
    private final String senderAddress;

    // SMTP is optional at startup; sending still requires a configured sender.
    public AdminEmailService(java.util.Optional<JavaMailSender> mailSender,
            @Value("${spring.mail.username:}") String senderAddress) {
        this.mailSender = mailSender.orElse(null);
        this.senderAddress = senderAddress;
    }

    // Send the administrator's message without changing its body.
    public void send(AdminEmailForm form) {
        if (mailSender == null || senderAddress.isBlank()) {
            throw new IllegalStateException("Email sender is not configured. Set QQ_MAIL_USERNAME and activate the qqmail profile.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(form.getRecipientEmail().trim());
        message.setSubject(form.getSubject().trim());
        message.setText(form.getBody());
        mailSender.send(message);
    }
}

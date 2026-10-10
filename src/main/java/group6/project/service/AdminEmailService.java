package group6.project.service;

import group6.project.form.AdminEmailForm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminEmailService {

    private final JavaMailSender mailSender;
    private final String senderAddress;

    // SMTP is optional at startup; sending still requires a configured sender.
    public AdminEmailService(
            Optional<JavaMailSender> mailSender,
            @Value("${cats.mail.from:}") String senderAddress,
            @Value("${spring.mail.username:}") String username) {
        this.mailSender = mailSender.orElse(null);
        this.senderAddress = senderAddress.isBlank() ? username : senderAddress;
    }

    // Send the administrator's message without changing its body.
    public void send(AdminEmailForm form) {
        if (mailSender == null || senderAddress.isBlank()) {
            throw new IllegalStateException(
                    "Email sender is not configured. Set SMTP credentials and activate the mail or"
                            + " qqmail profile.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(form.getRecipientEmail().trim());
        message.setSubject(form.getSubject().trim());
        message.setText(form.getBody());
        mailSender.send(message);
    }
}

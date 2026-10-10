package group6.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import group6.project.model.AdminEmailForm;

@Service
public class AdminEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String senderAddress;

    public AdminEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.username:}") String senderAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.senderAddress = senderAddress;
    }

    public void send(AdminEmailForm form) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (senderAddress.isBlank() || mailSender == null) {
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

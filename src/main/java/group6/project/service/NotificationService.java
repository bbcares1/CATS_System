// We prepare workflow emails and report delivery problems without undoing saved decisions.
package group6.project.service;

import group6.project.form.AdminEmailForm;
import group6.project.model.CourseApplication;
import group6.project.model.CourseFeeApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final AdminEmailService mail;
    private final String loginUrl;

    public NotificationService(
            AdminEmailService mail, @Value("${cats.public-url:http://localhost:8081}") String url) {
        this.mail = mail;
        this.loginUrl = url.replaceAll("/+$", "") + "/employee/login";
    }

    // Controllers call this after the application transaction has committed.
    public boolean submitted(CourseApplication course) {
        return send(
                course.getApprovalManager().getEmail(),
                "Course application for review",
                course.getApplicant().getName()
                        + " submitted or updated: "
                        + course.getCourseTitle()
                        + "\nDates: "
                        + course.getCourseStartDate()
                        + " to "
                        + course.getCourseEndDate()
                        + "\nJustification: "
                        + course.getJustification());
    }

    // Both decisions include the saved reason, so the email agrees with the history page.
    public boolean decided(CourseApplication course) {
        return send(
                course.getApplicant().getEmail(),
                "Course application " + course.getStatus().name().toLowerCase(),
                course.getCourseTitle()
                        + "\nDecision: "
                        + course.getStatus()
                        + "\nReason: "
                        + course.getDecisionReason());
    }

    // A claim uses the same notification flow without exposing its attachments by email.
    public boolean claimSubmitted(CourseFeeApplication claim) {
        return send(
                claim.getApprovalManager().getEmail(),
                "Fee claim for review",
                claim.getApplicant().getName()
                        + " submitted a claim for "
                        + claim.getCourseApplication().getCourseTitle()
                        + "\nAmount: $"
                        + claim.getAmount());
    }

    // A claim decision is approval or rejection, not confirmation of actual payment.
    public boolean claimDecided(CourseFeeApplication claim) {
        return send(
                claim.getApplicant().getEmail(),
                "Fee claim " + claim.getApplicationStatus().name().toLowerCase(),
                claim.getCourseApplication().getCourseTitle()
                        + "\nDecision: "
                        + claim.getApplicationStatus()
                        + "\nReason: "
                        + claim.getDecisionReason());
    }

    // Mail failure must not undo a saved request or invite a duplicate submission.
    private boolean send(String address, String subject, String text) {
        if (address == null || address.isBlank()) return false;
        AdminEmailForm form = new AdminEmailForm();
        form.setRecipientEmail(address);
        form.setSubject(subject);
        form.setBody(text + "\n\nLog in to CATS: " + loginUrl);
        try {
            mail.send(form);
            return true;
        } catch (MailException | IllegalStateException error) {
            logger.warn("CATS notification was not sent: {}", error.getClass().getSimpleName());
            return false;
        }
    }
}

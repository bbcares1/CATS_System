package group6.project.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminEmailForm {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Enter a valid recipient email address")
    @Size(max = 255, message = "Recipient email must be 255 characters or fewer")
    private String recipientEmail;

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must be 200 characters or fewer")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(max = 10000, message = "Message must be 10000 characters or fewer")
    private String body;
}

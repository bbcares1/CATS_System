// Collects editable account details without exposing saved passwords.
package group6.project.form;

import group6.project.model.Roles;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountForm {
    private Long version;

    @NotBlank
    @Pattern(
            regexp = "[A-Za-z0-9_.-]{3,100}",
            message = "Use 3–100 letters, digits, dots, underscores or hyphens for the username.")
    private String userName;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 255)
    private String staffId;

    @NotBlank(message = "Email is required for application notifications.")
    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 255)
    private String designation;

    @Size(max = 255)
    private String password;

    @NotNull private Roles role;
    private Integer managerId;
    private boolean active = true;
}

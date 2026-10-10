// Collects training-provider details.
package group6.project.form;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseProviderForm {
    private Long version;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    @Pattern(regexp = "|https?://[^\\s]+", message = "Use an http or https website address.")
    private String website;

    @Email
    @Size(max = 255)
    private String email;

    private boolean active = true;
}

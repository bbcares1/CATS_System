// Collects a public-holiday date and description.
package group6.project.form;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HolidayForm {
    private Long version;

    @NotNull(message = "Choose a holiday date.")
    private LocalDate date;

    @NotBlank(message = "Enter the holiday name.")
    @Size(max = 255)
    private String description;
}

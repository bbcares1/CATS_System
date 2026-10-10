// We collect course details and an optional date option in one submission.
package group6.project.form;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class CourseForm {
    private Long version;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    private java.math.BigDecimal courseFee = java.math.BigDecimal.ZERO;

    @Size(max = 2000)
    private String courseDescription;

    @NotNull private Integer categoryId;
    @NotNull private Integer providerId;
    private boolean customDatesAllowed;
    private boolean active = true;

    // A new fixed-date course can save its first schedule in the same form.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private java.time.LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private java.time.LocalDate endDate;

    @Pattern(regexp = "|AM|PM")
    private String halfDayPeriod;

    @Min(1)
    @Max(10000)
    private Integer capacity;

    private Double days;
}

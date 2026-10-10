package group6.project.form;

import group6.project.model.CourseCategoryType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

// Fields an employee may enter for a course outside the catalogue.
@Getter
@Setter
public class CourseApplicationForm {
    private Long version;
    @NotBlank @Size(max = 200)
    private String courseTitle;
    @NotNull
    private CourseCategoryType courseCategory;
    @NotBlank @Size(max = 200)
    private String trainingProvider;
    @NotNull
    private LocalDate courseStartDate;
    @NotNull
    private LocalDate courseEndDate;
    @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2)
    private BigDecimal courseFee = BigDecimal.ZERO;
    @NotBlank @Size(max = 2000)
    private String justification;
    @Size(max = 2000)
    private String workDissemination;
    @Pattern(regexp = "^(AM|PM)?$")
    private String halfDayPeriod;
    private Integer reviewerId;
}

package group6.project.form;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseForm {
    private Long version;
    @NotBlank @Size(max = 255)
    private String title;
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
    private java.math.BigDecimal courseFee = java.math.BigDecimal.ZERO;
    @Size(max = 2000)
    private String courseDescription;
    @NotNull
    private Integer categoryId;
    @NotNull
    private Integer providerId;
    private boolean customDatesAllowed;
    private boolean active = true;
}

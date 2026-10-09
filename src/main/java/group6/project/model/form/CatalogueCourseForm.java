package group6.project.model.form;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CatalogueCourseForm {
    private Long version;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 255)
    private String trainingProvider;

    @NotNull private Integer categoryId;

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal fee;

    @Size(max = 2000)
    private String description;

    private boolean customDatesAllowed;
    private boolean active = true;
}

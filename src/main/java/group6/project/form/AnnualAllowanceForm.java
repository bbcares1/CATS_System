// We collect one employee's training-day and budget limits for a selected year.
package group6.project.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AnnualAllowanceForm {
    @NotNull private Integer staffId;

    @Min(2000)
    @Max(2100)
    private int year;

    @NotNull
    @DecimalMin("0")
    private Double dayLimit;

    @NotNull
    @DecimalMin("0")
    private BigDecimal budget;
}

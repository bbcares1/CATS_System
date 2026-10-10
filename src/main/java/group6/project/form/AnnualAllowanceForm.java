package group6.project.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnualAllowanceForm {
    @NotNull private Integer staffId;
    @Min(2000) @Max(2100) private int year;
    @NotNull @DecimalMin("0") private Double dayLimit;
    @NotNull @DecimalMin("0") private BigDecimal budget;
}

package group6.project.model.form;

import java.time.LocalDate;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CatalogueBatchForm {
    private Long version;
    @NotNull private Integer courseId;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    private String halfDayPeriod;
    @NotNull @Min(1) @Max(100000) private Integer capacity;
    private boolean active = true;
}

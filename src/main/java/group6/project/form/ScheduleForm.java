package group6.project.form;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleForm {
    @NotNull
    private group6.project.model.CourseCategoryType category;
    @NotNull
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate startDate;
    @NotNull @DecimalMin("0.5") @DecimalMax("260")
    private Double days;
    @Pattern(regexp = "|AM|PM")
    private String halfDayPeriod;
}

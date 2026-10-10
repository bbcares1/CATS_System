package group6.project.form;

import group6.project.model.CourseCategoryType;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ScheduleForm {
    @NotNull private CourseCategoryType category;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private java.time.LocalDate startDate;

    @NotNull
    @DecimalMin("0.5")
    @DecimalMax("260")
    private Double days;

    @Pattern(regexp = "|AM|PM")
    private String halfDayPeriod;
}

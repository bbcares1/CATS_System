// We collect editable dates and capacity for a saved course schedule.
package group6.project.form;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class CourseBatchForm {
    private Long version;
    @NotNull private Integer courseId;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private java.time.LocalDate startDate;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private java.time.LocalDate endDate;

    @Pattern(regexp = "|AM|PM")
    private String halfDayPeriod;

    @NotNull
    @Min(1)
    @Max(10000)
    private Integer capacity;

    private boolean active = true;

    // Used only by the date calculator; saved training days are calculated again.
    private Double days;
}

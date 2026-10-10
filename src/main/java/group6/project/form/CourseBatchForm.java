package group6.project.form;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseBatchForm {
    private Long version;
    @NotNull
    private Integer courseId;
    @NotNull
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate startDate;
    @NotNull
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate endDate;
    @Pattern(regexp = "|AM|PM")
    private String halfDayPeriod;
    @NotNull @Min(1) @Max(10000)
    private Integer capacity;
    private boolean active = true;
}

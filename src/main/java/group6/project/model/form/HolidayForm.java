package group6.project.model.form;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
public class HolidayForm {
    private LocalDate date;
    private String description;
    private Long version;
}

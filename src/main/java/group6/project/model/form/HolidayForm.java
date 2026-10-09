package group6.project.model.form;

import java.time.LocalDate;
import lombok.*;

@Getter @Setter
public class HolidayForm {
    private LocalDate date;
    private String description;
    private Long version;
}

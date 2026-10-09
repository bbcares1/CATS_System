package group6.project.model.form;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CatalogueApplicationForm {
    private Long courseVersion;
    private Integer approvalManagerId;
    private Long applicationVersion;
    private String scheduledBatch;
    private LocalDate startDate;
    private LocalDate endDate;
    private String halfDayPeriod;
    private String justification;
    private String workDissemination;
}

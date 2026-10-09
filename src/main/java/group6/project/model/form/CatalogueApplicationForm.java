package group6.project.model.form;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

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

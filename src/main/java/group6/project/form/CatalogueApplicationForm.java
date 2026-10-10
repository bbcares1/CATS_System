package group6.project.form;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

// Catalogue title, category, provider and fee are always loaded by the service.
@Getter
@Setter
public class CatalogueApplicationForm {
    private Long version;
    private Long courseVersion;
    private Long batchId;
    private Long batchVersion;
    private LocalDate courseStartDate;
    private LocalDate courseEndDate;
    @Pattern(regexp = "^(AM|PM)?$")
    private String halfDayPeriod;
    @NotBlank @Size(max = 2000)
    private String justification;
    @Size(max = 2000)
    private String workDissemination;
    private Integer reviewerId;
}

// We collect dates and reasons for a catalogue application; the course supplies its details.
package group6.project.form;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

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

    @NotBlank
    @Size(max = 2000)
    private String justification;

    @Size(max = 2000)
    private String workDissemination;

    private Integer reviewerId;
}

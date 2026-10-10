// We collect the Manager's decision and required reason.
package group6.project.form;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DecisionForm {
    @NotNull private Long version;

    @NotBlank
    @Size(max = 2000)
    private String reason;

    @NotNull private Boolean approved;
}

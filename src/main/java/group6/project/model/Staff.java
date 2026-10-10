package group6.project.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("STAFF")
@Getter
@Setter
@NoArgsConstructor
public class Staff extends User {
    @NotNull(message = "Annual training budget is required")
    @DecimalMin(value = "0.0", message = "Annual training budget must be zero or greater")
    private Double trainingBudget;

    @NotNull(message = "Annual training days are required")
    @Min(value = 0, message = "Annual training days must be zero or greater")
    private Integer trainingDays;
}

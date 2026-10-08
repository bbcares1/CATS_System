package group6.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Staff extends User {

    @NotBlank(message = "Staff ID is required")
    private String staffId;

    @NotNull(message = "Annual training budget is required")
    @DecimalMin(value = "0.0", message = "Annual training budget must be zero or greater")
    private Double trainingBudget;

    @NotNull(message = "Annual training days are required")
    @Min(value = 0, message = "Annual training days must be zero or greater")
    private Integer trainingDays;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Staff manager;
}
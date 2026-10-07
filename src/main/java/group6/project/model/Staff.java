package group6.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    private String staffId;

    private Double trainingBudget;

    private Integer trainingDays;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Staff manager;
}
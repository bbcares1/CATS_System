package group6.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

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

    // Reporting manager: Each staff member reports to one manager.
    @ManyToOne
    @JoinColumn(name = "manager_id")
    @EqualsAndHashCode.Exclude
    private Manager manager;
}
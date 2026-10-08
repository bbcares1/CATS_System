package group6.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "training_entitlement")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class TrainingEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Year is required")
    @jakarta.persistence.Column(name = "`year`")
    private Integer year;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    public TrainingEntitlement(Integer year) {
        this.year = year;
    }
}

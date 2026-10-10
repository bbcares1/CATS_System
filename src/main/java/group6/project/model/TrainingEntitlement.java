package group6.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "training_entitlement",
        uniqueConstraints =
                @jakarta.persistence.UniqueConstraint(columnNames = {"staff_id", "year"}))
@Getter
@Setter
@NoArgsConstructor
public class TrainingEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Year is required")
    private Integer year;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Column(nullable = false)
    private Double dayLimit = 0d;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal budget = BigDecimal.ZERO;

    public TrainingEntitlement(Integer year) {
        this.year = year;
    }
}

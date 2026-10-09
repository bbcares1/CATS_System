package group6.project.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
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
    private Integer year;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(nullable = false)
    private Double dayLimit = 0d;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal budget = BigDecimal.ZERO;

    // Use one allowance record per employee and calendar year.
    public TrainingEntitlement(Integer year) {
        this.year = year;
    }
}

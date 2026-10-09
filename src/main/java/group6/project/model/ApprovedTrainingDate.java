package group6.project.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "approved_training_date",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"course_application_id", "training_date"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class ApprovedTrainingDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Associated course application
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "course_application_id",
        nullable = false
    )
    private CourseApplication courseApplication;

    // Actual confirmed training date
    @Column(name = "training_date", nullable = false)
    private LocalDate trainingDate;

    // 1.0 = full day, 0.5 = half day
    @Column(name = "training_duration", nullable = false)
    private Double trainingDuration = 1.0;

    public ApprovedTrainingDate(
            CourseApplication courseApplication,
            LocalDate trainingDate,
            Double trainingDuration) {

        this.courseApplication = courseApplication;
        this.trainingDate = trainingDate;
        this.trainingDuration = trainingDuration;
    }
}


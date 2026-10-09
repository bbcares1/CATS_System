package group6.project.model;

import java.time.LocalDate;

import group6.project.model.CourseDetail;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_batch")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CourseBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long batchId;

    @ManyToOne 
    @JoinColumn (name = "course_id")
    private CourseDetail courseDetail;

    private LocalDate courseStartDate;

    private LocalDate courseEndDate;

    private Double trainingDays;

    private Integer capacity;
}

package group6.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "course_batch")
@Getter
@Setter
@NoArgsConstructor
public class CourseBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long batchId;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private CourseDetail courseDetail;

    private LocalDate courseStartDate;

    private LocalDate courseEndDate;

    private Integer capacity;
    private String halfDayPeriod;
    private boolean active = true;

    @Version private Long version;
}

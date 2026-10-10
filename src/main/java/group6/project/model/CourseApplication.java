package group6.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_application")
@Getter
@Setter
@NoArgsConstructor
public class CourseApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer courseId;

    @jakarta.persistence.Version private Long version;

    @ManyToOne
    @JoinColumn(name = "catalogue_course_id")
    private CourseDetail catalogueCourse;

    @ManyToOne
    @JoinColumn(name = "catalogue_batch_id")
    private CourseBatch catalogueBatch;

    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne
    @JoinColumn(name = "approval_manager_id")
    private User approvalManager;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal courseFee = BigDecimal.ZERO;

    private LocalDate courseStartDate;

    private LocalDate courseEndDate;

    private String courseTitle;

    @Enumerated(EnumType.STRING)
    private CourseCategoryType courseCategory;

    private String trainingProvider;

    @Column(length = 2000)
    private String justification;

    @Column(length = 2000)
    private String workDissemination;

    private Double trainingDays;
    private String halfDayPeriod;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private User applicant;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime reviewedAt;

    @Column(length = 2000)
    private String decisionReason;

    @Column(length = 2000)
    private String experienceComments;
}

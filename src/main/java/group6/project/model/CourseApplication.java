
package group6.project.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.math.BigDecimal;
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
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_application")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CourseApplication {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer courseId;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    private Manager reviewer;

    // Catalogue references explain the source; the stored course fields remain the application snapshot.
    @ManyToOne
    @JoinColumn(name = "catalogue_course_id")
    private CourseDetail catalogueCourse;

    @ManyToOne
    @JoinColumn(name = "catalogue_batch_id")
    private CourseBatch catalogueBatch;

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
    private Staff applicant;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime reviewedAt;
    @Column(length = 2000)
    private String decisionReason;
    @Column(length = 2000)
    private String experienceComments;

    public ApplicationStatus getApplicationStatus() {
        return status;
    }

    public void setApplicationStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getReason() {
        return justification;
    }

    public void setReason(String reason) {
        this.justification = reason;
    }
}

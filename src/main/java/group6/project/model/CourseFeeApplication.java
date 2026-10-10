package group6.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_fee_application")
@Getter
@Setter
@NoArgsConstructor
public class CourseFeeApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer applicationId;

    @Version private Long version;

    @ManyToOne
    @JoinColumn(name = "approval_manager_id")
    private User approvalManager;

    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne
    @JoinColumn(name = "reimbursed_by_id")
    private User reimbursedBy;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    private String paymentReference;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private User applicant;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private CourseBatch courseBatch;

    @OneToOne
    @JoinColumn(name = "course_application_id", unique = true)
    private CourseApplication courseApplication;

    private LocalDateTime reimbursedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus applicationStatus;

    @Lob
    @Column(name = "receipt", length = 5242880)
    private byte[] receipt;

    private String receiptFileName;

    private String receiptContentType;

    @Lob
    @Column(name = "certificate", length = 5242880)
    private byte[] certificate;

    private String certificateFileName;

    private String certificateContentType;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @Column(length = 2000)
    private String decisionReason;
}

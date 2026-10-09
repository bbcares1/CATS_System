package group6.project.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

import org.hibernate.annotations.JoinColumnOrFormula;

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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_fee_application")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CourseFeeApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer applicationId;

    @ManyToOne 
    @JoinColumn (name = "staff_id")
    private User applicant;

    @ManyToOne 
    @JoinColumn (name = "batch_id")
    private CourseBatch courseBatch;
    @OneToOne
    @JoinColumn(name = "course_application_id", unique = true)
    private CourseApplication courseApplication;

    private LocalDateTime reimbursedAt;

    @Enumerated (EnumType.STRING)
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

    private String decisionReason;  

}

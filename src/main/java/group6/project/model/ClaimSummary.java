package group6.project.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// List projections omit evidence bytes; downloads load one authorized claim separately.
public interface ClaimSummary {
    Integer getApplicationId();

    String getApplicantName();

    String getStaffId();

    String getCourseTitle();

    BigDecimal getAmount();

    ApplicationStatus getApplicationStatus();

    Long getVersion();

    LocalDateTime getSubmittedAt();

    LocalDateTime getReviewedAt();

    LocalDateTime getReimbursedAt();

    String getReviewerName();

    String getPaymentReference();

    boolean getLegacyClaim();
}

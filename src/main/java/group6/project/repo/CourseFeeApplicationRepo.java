package group6.project.repo;

import group6.project.model.ApplicationStatus;
import group6.project.model.ClaimSummary;
import group6.project.model.CourseFeeApplication;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseFeeApplicationRepo extends JpaRepository<CourseFeeApplication, Integer> {

    List<CourseFeeApplication> findByApplicant_UserId(Integer userId);

    boolean existsByApplicant_UserId(Integer userId);

    boolean existsByApplicant_UserIdAndApplicationStatus(Integer userId, ApplicationStatus status);

    boolean existsByCourseApplication_CourseId(Integer id);

    boolean existsByApprovalManager_UserIdAndApplicationStatus(
            Integer id, ApplicationStatus status);

    boolean existsByApplicant_UserIdOrReviewer_UserIdOrApprovalManager_UserIdOrReimbursedBy_UserId(
            Integer a, Integer b, Integer c, Integer d);

    // Projections and pagination keep multi-megabyte attachments out of all lists.
    String SUMMARY =
            """
            select c.applicationId as applicationId, u.name as applicantName, u.staffId as staffId,
            a.courseTitle as courseTitle, c.amount as amount, c.applicationStatus as applicationStatus,
            c.version as version, c.submittedAt as submittedAt, c.reviewedAt as reviewedAt,
            c.reimbursedAt as reimbursedAt, r.name as reviewerName, c.paymentReference as paymentReference
            from CourseFeeApplication c join c.applicant u join c.courseApplication a left join c.reviewer r
            """;

    @Query(
            value =
                    SUMMARY
                            + "where c.applicant.userId=:id order by c.submittedAt"
                            + " desc,c.applicationId desc",
            countQuery = "select count(c) from CourseFeeApplication c where c.applicant.userId=:id")
    Page<ClaimSummary> personal(@Param("id") Integer id, Pageable page);

    @Query(
            value =
                    SUMMARY
                            + "where c.approvalManager.userId=:id and c.applicationStatus=:status"
                            + " order by c.submittedAt,c.applicationId",
            countQuery =
                    "select count(c) from CourseFeeApplication c where c.approvalManager.userId=:id"
                            + " and c.applicationStatus=:status")
    Page<ClaimSummary> pending(
            @Param("id") Integer id, @Param("status") ApplicationStatus status, Pageable page);

    @Query(
            value =
                    SUMMARY
                            + "where c.applicationStatus=:status order by"
                            + " c.reimbursedAt,c.reviewedAt,c.applicationId",
            countQuery =
                    "select count(c) from CourseFeeApplication c where c.applicationStatus=:status")
    Page<ClaimSummary> approved(@Param("status") ApplicationStatus status, Pageable page);

    // Reimbursement is recorded once and does not consume course budget again.
    @Query(
            "select coalesce(sum(c.amount),0) from CourseFeeApplication c where"
                    + " c.applicant.userId=:id and c.reimbursedAt is not null and"
                    + " c.courseApplication.courseStartDate between :from and :to")
    java.math.BigDecimal reimbursed(
            @Param("id") Integer id,
            @Param("from") java.time.LocalDate from,
            @Param("to") java.time.LocalDate to);

    @Query("select c.applicant.userId from CourseFeeApplication c where c.applicationId=:id")
    Optional<Integer> applicantId(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CourseFeeApplication c where c.applicationId=:id")
    Optional<CourseFeeApplication> lockById(@Param("id") Integer id);

    // Reports need payment totals, never the receipt/certificate blobs.
    @Query(
            "select c.courseApplication.courseId as courseId, c.applicationStatus as status,"
                + " c.amount as amount, c.reimbursedAt as paidAt from CourseFeeApplication c where"
                + " c.courseApplication.courseId in :ids")
    List<ClaimTotal> totalsForCourses(@Param("ids") List<Integer> ids);

    interface ClaimTotal {
        Integer getCourseId();

        ApplicationStatus getStatus();

        java.math.BigDecimal getAmount();

        java.time.LocalDateTime getPaidAt();
    }
}

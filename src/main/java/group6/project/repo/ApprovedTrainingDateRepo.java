package group6.project.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import group6.project.model.ApprovedTrainingDate;

public interface ApprovedTrainingDateRepo
        extends JpaRepository<ApprovedTrainingDate, Integer> {

    // Find approved training dates within a selected month
    @Query("""
            SELECT d
            FROM ApprovedTrainingDate d
            JOIN FETCH d.courseApplication a
            JOIN FETCH a.applicant s
            WHERE a.status = group6.project.model.ApplicationStatus.APPROVED
              AND d.trainingDate BETWEEN :startDate AND :endDate
            ORDER BY d.trainingDate, a.courseId
            """)
    List<ApprovedTrainingDate> findApprovedTrainingDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Find all saved dates for a course application
    List<ApprovedTrainingDate> findByCourseApplication_CourseIdOrderByTrainingDateAsc(
            Integer courseId);
}

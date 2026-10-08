package group6.project.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;

public interface CourseApplicationRepo extends JpaRepository<CourseApplication,Integer>{
    List<CourseApplication> findByApplicant_UserIdAndCourseStartDateBetweenOrderByCourseStartDateAsc(
            Integer userId, java.time.LocalDate from, java.time.LocalDate to);

    List<CourseApplication> findByApplicant_Manager_UserIdAndStatusInOrderBySubmittedAtAsc(
            Integer managerId, List<ApplicationStatus> statuses);

    List<CourseApplication> findByApplicant_UserIdOrderByCourseStartDateDesc(Integer staffId);

    List<CourseApplication> findByApplicant_UserIdAndStatusIn(
            Integer userId, List<ApplicationStatus> statuses);
}

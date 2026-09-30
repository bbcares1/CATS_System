package group6.project.repo;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.ExcludedDays;

public interface ExcludedDaysRepo extends JpaRepository<ExcludedDays, Long> {
  
  boolean existsByHolidayDate(LocalDate holidayDate);

}

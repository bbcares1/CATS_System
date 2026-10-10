package group6.project.repo;

import group6.project.model.ExcludedDays;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ExcludedDaysRepo extends JpaRepository<ExcludedDays, Integer> {

    boolean existsByDate(LocalDate date);

    boolean existsByDateAndIdNot(LocalDate date, Integer id);
}

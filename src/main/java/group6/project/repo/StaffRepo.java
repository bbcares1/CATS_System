package group6.project.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import group6.project.model.Staff;

public interface StaffRepo extends JpaRepository<Staff, Integer> {
}
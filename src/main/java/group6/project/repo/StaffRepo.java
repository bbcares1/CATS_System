package group6.project.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import group6.project.model.Staff;

public interface StaffRepo extends JpaRepository<Staff, Integer> {
    Optional<Staff> findByUserName(String userName);
}
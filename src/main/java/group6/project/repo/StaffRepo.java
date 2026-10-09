package group6.project.repo;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import group6.project.model.Staff;

public interface StaffRepo extends JpaRepository<Staff, Integer> {
    Optional<Staff> findByUserName(String userName);

    // Reporting manager - Find staff who report to this manager.
    List<Staff> findByManager_UserId(Integer managerId);
}
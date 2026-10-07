package group6.project.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.Manager;

public interface ManagerRepo extends JpaRepository<Manager, Integer> {

    Optional<Manager> findByStaffId(String staffId);

    Optional<Manager> findByUserName(String userName);
}

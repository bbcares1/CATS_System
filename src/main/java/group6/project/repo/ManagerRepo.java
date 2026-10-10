// We query Manager accounts and find Managers by their staff identifier.
package group6.project.repo;

import group6.project.model.Manager;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagerRepo extends JpaRepository<Manager, Integer> {

    Optional<Manager> findByStaffId(String staffId);

    Optional<Manager> findByUserName(String userName);
}

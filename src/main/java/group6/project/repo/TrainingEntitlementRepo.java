// Queries and locks an employee's allowance for a selected year.
package group6.project.repo;

import group6.project.model.TrainingEntitlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingEntitlementRepo extends JpaRepository<TrainingEntitlement, Integer> {

    List<TrainingEntitlement> findByStaff_UserId(Integer staffDbId);

    Optional<TrainingEntitlement> findByStaff_UserIdAndYear(Integer staffDbId, Integer year);
}

package group6.project.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.TrainingEntitlement;

public interface TrainingEntitlementRepo
        extends JpaRepository<TrainingEntitlement, Integer> {

    List<TrainingEntitlement> findByStaff_Id(Integer staffDbId);

    Optional<TrainingEntitlement> findByStaff_IdAndYear(
            Integer staffDbId,
            Integer year);
}
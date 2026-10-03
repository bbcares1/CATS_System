package group6.project.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.TrainingEntitlement;
import java.util.List;
import java.util.Optional;

public interface  TrainingEntitlementRepo extends JpaRepository<TrainingEntitlement, Integer> {

  List<TrainingEntitlement> findByStaff_Id(Integer staffDId);

  Optional<TrainingEntitlement> findByStaff_IdAndYear(Integer staffDId,
  Integer year);

}

package group6.project.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.TrainingEntitlement;

public interface  TrainingEntitlementRepo extends JpaRepository<TrainingEntitlement, Integer> {
      
}

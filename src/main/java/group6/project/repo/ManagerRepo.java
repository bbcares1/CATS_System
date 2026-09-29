package group6.project.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.Manager;

public interface ManagerRepo extends JpaRepository<Manager, Integer> {

}

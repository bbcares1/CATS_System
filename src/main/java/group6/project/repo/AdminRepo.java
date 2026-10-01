package group6.project.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.Admin;

public interface AdminRepo extends JpaRepository<Admin,Integer>{

      Optional<Admin> findByUserName(String userName);
      Optional<Admin> findByStaffNo(String staffNo);
    
}

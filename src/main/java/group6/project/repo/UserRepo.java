package group6.project.repo;

import group6.project.model.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User,Integer>{
    Optional<User> findByUserName(String userName);
}

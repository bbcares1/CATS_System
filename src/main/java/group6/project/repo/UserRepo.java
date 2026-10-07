package group6.project.repo;

import group6.project.model.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User,Integer>{
    java.util.Optional<User> findByUserName(String userName);
}

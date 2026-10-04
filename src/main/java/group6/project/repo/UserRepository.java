package group6.project.repo;

import group6.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Integer> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByStaffNo(String staffNo);
}

package group6.project.repo;

import group6.project.model.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepo extends JpaRepository<User, Integer> {
    Optional<User> findByUserName(String userName);
    Optional<User> findByUserNameIgnoreCase(String userName);
    Optional<User> findByEmail(String email);

    // Account maintenance shares one lock order for this small team application.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u order by u.userId")
    List<User> lockAccounts();

    // Reload after changing the discriminator; a managed Staff object cannot become an Admin.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update users set role=:role, version=version+1, training_days=coalesce(training_days,0), training_budget=coalesce(training_budget,0) where user_id=:id", nativeQuery = true)
    int changeRole(@Param("id") Integer id, @Param("role") String role);

    boolean existsByUserNameIgnoreCaseAndUserIdNot(String name, Integer id);
    boolean existsByStaffIdIgnoreCaseAndUserIdNot(String staffId, Integer id);
    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, Integer id);
}

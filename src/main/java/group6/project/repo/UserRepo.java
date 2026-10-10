package group6.project.repo;

import group6.project.model.Roles;
import group6.project.model.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Integer> {
    Optional<User> findByUserName(String userName);

    Optional<User> findByUserNameIgnoreCase(String userName);

    boolean existsByRoleAndActiveTrue(Roles role);

    Optional<User> findByEmail(String email);

    // Account maintenance shares one lock order for this small team application.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u order by u.userId")
    List<User> lockAccounts();

    // Read the reporting ID without loading an account before its write lock.
    @Query("select u.manager.userId from User u where u.userId=:id")
    Optional<Integer> reportingManagerId(@Param("id") Integer id);

    // A consistent order also handles two Managers applying to each other.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId in :ids order by u.userId")
    List<User> lockParticipants(@Param("ids") List<Integer> ids);

    // Reload after changing the discriminator; a managed Staff object cannot become an Admin.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = "update users set role=:role, version=version+1 where user_id=:id",
            nativeQuery = true)
    int changeRole(@Param("id") Integer id, @Param("role") String role);

    boolean existsByUserNameIgnoreCaseAndUserIdNot(String name, Integer id);

    boolean existsByStaffIdIgnoreCaseAndUserIdNot(String staffId, Integer id);

    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, Integer id);
}

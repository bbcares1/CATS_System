package group6.project.repo;

import group6.project.model.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Integer> {
    // Bootstrap runs once when no active production Admin exists.
    boolean existsByRoleAndActiveTrue(group6.project.model.Roles role);

    Optional<User> findByUserName(String userName);

    boolean existsByUserNameIgnoreCaseAndUserIdNot(String name, Integer id);

    boolean existsByStaffIdIgnoreCaseAndUserIdNot(String staffId, Integer id);

    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, Integer id);

    // Serialize small-team account edits so role assignments and last-Admin checks cannot race.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u order by u.userId")
    List<User> lockAccounts();

    @Query("select u.manager.userId from User u where u.userId=:id")
    Optional<Integer> reportingManagerId(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId in :ids order by u.userId")
    List<User> lockParticipants(@Param("ids") List<Integer> ids);

    // Changing the discriminator requires clearing managed objects before loading the new subtype.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = "update users set role=:role, version=version+1 where user_id=:id",
            nativeQuery = true)
    int changeRole(@Param("id") Integer id, @Param("role") String role);
}

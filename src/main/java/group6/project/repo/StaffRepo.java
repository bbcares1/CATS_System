// We query employees and their reporting relationships.
package group6.project.repo;

import group6.project.model.Staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffRepo extends JpaRepository<Staff, Integer> {
    // Serialize writes that can change one employee's annual allowance use.
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Staff s where s.userId = :id")
    Optional<Staff> lockById(@Param("id") Integer id);

    Optional<Staff> findByUserName(String userName);

    // Reporting manager - Find staff who report to this manager.
    List<Staff> findByManager_UserId(Integer managerId);
}

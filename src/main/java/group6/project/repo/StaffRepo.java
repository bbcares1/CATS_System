package group6.project.repo;

import java.util.Optional;
import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import group6.project.model.Staff;

public interface StaffRepo extends JpaRepository<Staff, Integer> {
    // Serialize allowance changes and submissions for the same employee.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Staff s where s.userId = :id")
    Optional<Staff> lockById(@Param("id") Integer id);

    Optional<Staff> findByUserName(String userName);

    // Reporting manager - Find staff who report to this manager.
    List<Staff> findByManager_UserId(Integer managerId);
}
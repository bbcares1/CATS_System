package group6.project.repo;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import group6.project.model.Staff;

public interface StaffRepo extends JpaRepository<Staff, Integer> {
    // Serialize writes that can change one employee's annual allowance use.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Staff s where s.userId = :id")
    Optional<Staff> lockById(@org.springframework.data.repository.query.Param("id") Integer id);

    Optional<Staff> findByUserName(String userName);

    // Reporting manager - Find staff who report to this manager.
    List<Staff> findByManager_UserId(Integer managerId);
}
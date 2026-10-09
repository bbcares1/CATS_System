package group6.project.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import group6.project.model.Staff;
import group6.project.model.TrainingEntitlement;
import group6.project.repo.StaffRepo;
import group6.project.repo.TrainingEntitlementRepo;

@Service
public class TrainingEntitlementService {
    private final TrainingEntitlementRepo entitlements;
    private final StaffRepo employees;
    private final CourseApplicationService policy;

    // Use the shared policy for totals rather than another allowance calculation.
    public TrainingEntitlementService(TrainingEntitlementRepo entitlements, StaffRepo employees,
            CourseApplicationService policy) {
        this.entitlements = entitlements;
        this.employees = employees;
        this.policy = policy;
    }

    // Each list row contains only one employee's selected-year totals.
    public List<AllowanceRow> rows(int year) {
        validateYear(year);
        return employees.findAll().stream().map(staff -> new AllowanceRow(staff,
                policy.summaryForYear(staff, year, null))).toList();
    }

    // A missing year's allowance is zero until Admin explicitly allocates it.
    public CourseApplicationService.Summary summary(Integer employeeId, int year) {
        validateYear(year);
        return policy.summaryForYear(employee(employeeId), year, null);
    }

    // Use database identity rather than the editable employee identifier for relations.
    public Staff employee(Integer id) {
        return employees.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee was not found."));
    }

    // Save one year's allowance; reducing it must not invalidate saved applications.
    @Transactional
    public void saveLimits(Integer employeeId, int year, double days, BigDecimal budget) {
        validateYear(year);
        if (!Double.isFinite(days) || days < 0 || days > 366 || days * 2 != Math.floor(days * 2)
                || budget == null || budget.signum() < 0 || budget.scale() > 2
                || budget.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Use non-negative half-day limits and a budget with at most two decimal places.");
        }
        Staff staff = employees.lockById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee was not found."));
        var used = policy.summaryForYear(staff, year, null);
        if (days < used.usedDays() || budget.compareTo(used.usedBudget()) < 0) {
            throw new IllegalArgumentException("The allowance cannot be lower than the employee's reserved days or fees.");
        }
        TrainingEntitlement allowance = entitlements.findByStaff_UserIdAndYear(employeeId, year)
                .orElseGet(() -> new TrainingEntitlement(year));
        allowance.setStaff(staff);
        allowance.setDayLimit(days);
        allowance.setBudget(budget);
        entitlements.save(allowance);
    }

    // Keep submitted years within a sensible range and show validation instead of a database error.
    private void validateYear(int year) {
        if (year < 2000 || year > 2100) throw new IllegalArgumentException("Year must be between 2000 and 2100.");
    }

    public record AllowanceRow(Staff staff, CourseApplicationService.Summary summary) {}
}

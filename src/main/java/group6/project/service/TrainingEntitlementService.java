// We calculate yearly reserved, used and remaining allowances for every workspace.
package group6.project.service;

import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;
import group6.project.model.Staff;
import group6.project.model.TrainingEntitlement;
import group6.project.model.User;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.StaffRepo;
import group6.project.repo.TrainingEntitlementRepo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TrainingEntitlementService {
    private final TrainingEntitlementRepo entitlements;
    private final StaffRepo employees;
    private final CourseApplicationRepo applications;

    public TrainingEntitlementService(
            TrainingEntitlementRepo entitlements,
            StaffRepo employees,
            CourseApplicationRepo applications) {
        this.entitlements = entitlements;
        this.employees = employees;
        this.applications = applications;
    }

    // A missing allocation means zero available days and budget for that year.
    public AnnualSummary summary(User employee, int year, Integer excludedId) {
        validateYear(year);
        TrainingEntitlement allowance =
                entitlements
                        .findByStaff_UserIdAndYear(employee.getUserId(), year)
                        .orElse(new TrainingEntitlement(year));
        double reservedDays = 0;
        double usedDays = 0;
        BigDecimal reservedBudget = BigDecimal.ZERO;
        BigDecimal usedBudget = BigDecimal.ZERO;
        List<CourseApplication> courses =
                applications
                        .findByApplicant_UserIdAndCourseStartDateBetweenOrderByCourseStartDateAsc(
                                employee.getUserId(),
                                LocalDate.of(year, 1, 1),
                                LocalDate.of(year, 12, 31));
        for (CourseApplication course : courses) {
            if (excludedId != null && excludedId.equals(course.getCourseId())) continue;
            double days = course.getTrainingDays() == null ? 0 : course.getTrainingDays();
            if (course.getStatus() == ApplicationStatus.APPLIED
                    || course.getStatus() == ApplicationStatus.UPDATED) {
                reservedDays += days;
                reservedBudget = reservedBudget.add(course.getCourseFee());
            } else if (course.getStatus() == ApplicationStatus.APPROVED
                    || course.getStatus() == ApplicationStatus.COMPLETED) {
                usedDays += days;
                usedBudget = usedBudget.add(course.getCourseFee());
            }
        }
        return new AnnualSummary(
                allowance.getDayLimit(),
                allowance.getBudget(),
                reservedDays,
                reservedBudget,
                usedDays,
                usedBudget);
    }

    // Prepare one table row per employee without mixing different years.
    public List<AllowanceRow> rows(int year) {
        validateYear(year);
        List<AllowanceRow> rows = new ArrayList<>();
        for (Staff employee : employees.findAll()) {
            rows.add(new AllowanceRow(employee, summary(employee, year, null)));
        }
        return rows;
    }

    // Database IDs keep links valid when an editable Staff ID changes.
    public Staff employee(Integer id) {
        return employees
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Employee not found."));
    }

    // The edit page distinguishes an unset allowance from an intentional zero limit.
    public boolean hasAllowance(Integer employeeId, int year) {
        return entitlements.findByStaff_UserIdAndYear(employeeId, year).isPresent();
    }

    // These classroom defaults are suggestions; Admin saves the actual yearly allocation.
    public double suggestedDays(String designation) {
        if ("Administrative".equalsIgnoreCase(designation)) return 5;
        if ("Professional".equalsIgnoreCase(designation)) return 10;
        return 0;
    }

    // Lock the employee so a limit edit and a new application cannot spend the same allowance.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void saveLimits(Integer employeeId, int year, double days, BigDecimal budget) {
        validateYear(year);
        if (!Double.isFinite(days)
                || days < 0
                || days > 366
                || days * 2 != Math.floor(days * 2)
                || budget == null
                || budget.signum() < 0
                || budget.scale() > 2
                || budget.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use a half-day limit from 0 to 366 and a non-negative budget with up to two"
                            + " decimals.");
        }
        Staff employee =
                employees
                        .lockById(employeeId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Employee not found."));
        AnnualSummary total = summary(employee, year, null);
        if (days < total.totalDays() || budget.compareTo(total.totalBudget()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The allowance cannot be lower than the days or fees already reserved or"
                            + " used.");
        }
        TrainingEntitlement allowance =
                entitlements
                        .findByStaff_UserIdAndYear(employeeId, year)
                        .orElse(new TrainingEntitlement(year));
        allowance.setStaff(employee);
        allowance.setDayLimit(days);
        allowance.setBudget(budget);
        entitlements.save(allowance);
    }

    // Bound a year before using it in dates or a database query.
    public void validateYear(int year) {
        if (year < 2000 || year > 2100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Year must be between 2000 and 2100.");
        }
    }

    public record AllowanceRow(Staff staff, AnnualSummary summary) {}

    public record AnnualSummary(
            double dayLimit,
            BigDecimal budget,
            double reservedDays,
            BigDecimal reservedBudget,
            double usedDays,
            BigDecimal usedBudget) {
        // Pending requests reserve allowance; approved and completed courses use it.
        public double totalDays() {
            return reservedDays + usedDays;
        }

        public BigDecimal totalBudget() {
            return reservedBudget.add(usedBudget);
        }

        public double remainingDays() {
            return Math.max(0, dayLimit - totalDays());
        }

        public BigDecimal remainingBudget() {
            return budget.subtract(totalBudget()).max(BigDecimal.ZERO);
        }
    }
}

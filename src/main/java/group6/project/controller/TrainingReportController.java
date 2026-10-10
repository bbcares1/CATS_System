// Shows filtered training reports and exports their records to CSV.
package group6.project.controller;

import group6.project.model.*;
import group6.project.service.*;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Controller
public class TrainingReportController {
    private final TrainingReportService reports;
    private final UserService users;

    public TrainingReportController(TrainingReportService reports, UserService users) {
        this.reports = reports;
        this.users = users;
    }

    // Include courses overlapping the period; annual allowances retain their full-year meaning.
    @GetMapping({"/admin/reports", "/manager/reports"})
    public String page(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) CourseCategoryType category,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(defaultValue = "false") boolean attendanceOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        User actor = actor(session);
        from = from == null ? LocalDate.now().withDayOfYear(1) : from;
        to = to == null ? LocalDate.of(from.getYear(), 12, 31) : to;
        var report = load(actor, from, to, category, employeeId, attendanceOnly);
        if (size != 10 && size != 20 && size != 25) size = 10;
        int last = Math.max(0, (report.rows().size() - 1) / size);
        page = Math.max(0, Math.min(page, last));
        int start = page * size;
        model.addAttribute(
                "rows", report.rows().subList(start, Math.min(start + size, report.rows().size())));
        model.addAttribute("report", report);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("category", category);
        model.addAttribute("employeeId", employeeId);
        model.addAttribute("categories", CourseCategoryType.values());
        model.addAttribute("attendanceOnly", attendanceOnly);
        model.addAttribute("page", page);
        model.addAttribute("lastPage", last);
        model.addAttribute("size", size);
        model.addAttribute("workspace", actor instanceof Admin ? "Admin" : "Manager");
        model.addAttribute("route", actor instanceof Admin ? "/admin/reports" : "/manager/reports");
        return "training-report";
    }

    // CSV uses the same filters and access checks as the page, without the page-size limit.
    @GetMapping({"/admin/reports.csv", "/manager/reports.csv"})
    public ResponseEntity<byte[]> csv(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) CourseCategoryType category,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(defaultValue = "false") boolean attendanceOnly,
            HttpSession session) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(
                        "Content-Disposition",
                        ContentDisposition.attachment()
                                .filename("cats-training-" + from + "-" + to + ".csv")
                                .build()
                                .toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(
                        reports.csv(
                                load(
                                        actor(session),
                                        from,
                                        to,
                                        category,
                                        employeeId,
                                        attendanceOnly)));
    }

    // Invalid dates return a useful client error rather than a repository or template exception.
    private TrainingReportService.Report load(
            User actor,
            LocalDate from,
            LocalDate to,
            CourseCategoryType category,
            Integer employeeId,
            boolean attendanceOnly) {
        try {
            return reports.report(actor, from, to, category, employeeId, attendanceOnly);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // An expired or changed account cannot export data using an old session.
    private User actor(HttpSession session) {
        var user = users.currentUser(session);
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return user;
    }
}

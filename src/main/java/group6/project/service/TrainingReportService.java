package group6.project.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import group6.project.model.*;
import group6.project.repo.*;

@Service
public class TrainingReportService {
    private final UserRepo users;
    private final CourseApplicationRepo applications;
    private final CourseFeeApplicationRepo claims;
    private final CourseApplicationService policy;

    // Report totals reuse saved applications and the shared annual allowance policy.
    public TrainingReportService(UserRepo users,CourseApplicationRepo applications,CourseFeeApplicationRepo claims,CourseApplicationService policy) {
        this.users=users;this.applications=applications;this.claims=claims;this.policy=policy;
    }

    // Admin sees all accounts; Managers see current direct reports only, never another team.
    @Transactional(readOnly=true)
    public Report report(User actor,LocalDate from,LocalDate to,CourseCategoryType category,Integer employeeId,boolean attendanceOnly) {
        if(!(actor instanceof Admin) && !(actor instanceof Manager)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(from==null || to==null || to.isBefore(from) || from.getYear()!=to.getYear() || from.getYear()<2000 || to.getYear()>2100) {
            throw new IllegalArgumentException("Choose an ordered date range within one calendar year (2000–2100).");
        }
        var scope=users.findAll().stream().filter(u->actor instanceof Admin || u.getManager()!=null
                && actor.getUserId().equals(u.getManager().getUserId()) && !actor.getUserId().equals(u.getUserId()))
                .sorted(Comparator.comparing(User::getName)).toList();
        if(employeeId!=null && scope.stream().noneMatch(u->employeeId.equals(u.getUserId()))) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var selected=scope.stream().filter(u->employeeId==null || employeeId.equals(u.getUserId())).toList();
        var records=selected.isEmpty()?List.<CourseApplication>of():applications.findForReport(
                selected.stream().map(User::getUserId).toList(),from,to).stream()
                .filter(a->category==null || category==a.getCourseCategory())
                .filter(a->!attendanceOnly || a.getStatus()==ApplicationStatus.APPROVED || a.getStatus()==ApplicationStatus.COMPLETED).toList();
        Map<Integer,CourseFeeApplicationRepo.ClaimTotal> payments=new HashMap<>();
        if(!records.isEmpty()) for(var c:claims.totalsForCourses(records.stream().map(CourseApplication::getCourseId).toList())) payments.put(c.getCourseId(),c);
        List<Row> rows=new ArrayList<>();
        for(var a:records) {
            var c=payments.get(a.getCourseId());
            rows.add(new Row(a.getApplicant().getName(),a.getApplicant().getStaffId(),a.getCourseTitle(),a.getCourseCategory(),
                    a.getCourseStartDate(),a.getCourseEndDate(),a.getTrainingDays(),a.getCourseFee(),a.getStatus(),
                    c==null?null:c.getStatus(),c!=null && c.getStatus()==ApplicationStatus.APPROVED?c.getAmount():BigDecimal.ZERO,
                    c!=null && c.getPaidAt()!=null?c.getAmount():BigDecimal.ZERO));
        }
        var annual=selected.stream().map(u->new Annual(u.getName(),u.getStaffId(),policy.summaryForYear(u,from.getYear(),null))).toList();
        BigDecimal committed=rows.stream().filter(r->r.status()==ApplicationStatus.APPROVED || r.status()==ApplicationStatus.COMPLETED)
                .map(Row::fee).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal approved=rows.stream().map(Row::approvedClaim).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal paid=rows.stream().map(Row::reimbursed).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new Report(List.copyOf(rows),scope.stream().map(u->new Employee(u.getUserId(),u.getName(),u.getStaffId())).toList(),annual,committed,approved,paid);
    }

    // Export the filtered rows, including unpaginated results and distinct approval/payment amounts.
    public byte[] csv(Report report) {
        StringBuilder csv=new StringBuilder("\uFEFFEmployee,Staff ID,Course,Category,Start,End,Full course days,Full course fee,Status,Claim status,Approved claim,Reimbursed\r\n");
        for(Row r:report.rows()) {
            Object[] cells={r.employee(),r.staffId(),r.title(),r.category()==null?"":r.category().getDisplayName(),r.start(),r.end(),
                    r.days(),r.fee(),r.status(),r.claimStatus(),r.approvedClaim(),r.reimbursed()};
            csv.append(Arrays.stream(cells).map(this::csvCell).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // Quote every cell and neutralize spreadsheet formulas supplied in names or course titles.
    private String csvCell(Object value) {
        String text=value==null?"":value.toString();String leading=text.stripLeading();
        if(!leading.isEmpty() && "=+-@".indexOf(leading.charAt(0))>=0) text="'"+text;
        return "\""+text.replace("\"","\"\"")+"\"";
    }

    public record Row(String employee,String staffId,String title,CourseCategoryType category,LocalDate start,LocalDate end,
            Double days,BigDecimal fee,ApplicationStatus status,ApplicationStatus claimStatus,BigDecimal approvedClaim,BigDecimal reimbursed) {}
    public record Employee(Integer id,String name,String staffId) {}
    public record Annual(String name,String staffId,CourseApplicationService.Summary summary) {}
    public record Report(List<Row> rows,List<Employee> employees,List<Annual> annual,BigDecimal committedFees,BigDecimal approvedClaims,BigDecimal reimbursed) {}
}

package group6.project.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import group6.project.model.*;
import group6.project.repo.*;

@Service
public class CourseFeeApplicationService {
    private final CourseFeeApplicationRepo claims;
    private final CourseApplicationService courses;
    private final CourseApplicationRepo applications;
    private final ApprovalRoutingService routing;

    // Keep eligibility, claim decisions and payment recording in one service.
    public CourseFeeApplicationService(CourseFeeApplicationRepo claims, CourseApplicationService courses,
            CourseApplicationRepo applications, ApprovalRoutingService routing) {
        this.claims=claims; this.courses=courses; this.applications=applications; this.routing=routing;
    }

    // Each completed External/Certification application can have one claim, even if it was rejected.
    public List<CourseApplication> eligible(User employee) {
        return applications.findByApplicant_UserIdAndStatusIn(employee.getUserId(), List.of(ApplicationStatus.COMPLETED)).stream()
                .filter(c -> c.getCourseCategory()!=CourseCategoryType.INTERNAL_TRAINING && c.getCourseFee().signum()>0
                        && !claims.existsByCourseApplication_CourseId(c.getCourseId())).toList();
    }

    // Ordered account locks prevent duplicate submissions and keep the assigned reviewer stable.
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public CourseFeeApplication submit(Integer courseId, boolean paidPersonally, MultipartFile receipt,
            MultipartFile certificate, User employee, Integer selectedReviewer) {
        User reviewer=routing.forSubmission(employee, selectedReviewer);
        CourseApplication course=courses.getOwned(courseId,employee);
        if (!paidPersonally) throw new IllegalArgumentException("Only personally paid course fees can be claimed.");
        if (course.getStatus()!=ApplicationStatus.COMPLETED || course.getCourseCategory()==CourseCategoryType.INTERNAL_TRAINING
                || course.getCourseFee().signum()<=0) throw new IllegalArgumentException("Only completed fee-paying External Courses or Certifications can be claimed.");
        if (claims.existsByCourseApplication_CourseId(courseId)) throw new IllegalArgumentException("A claim already exists for this course.");
        Upload proof=read(receipt), completion=read(certificate);
        CourseFeeApplication claim=new CourseFeeApplication();
        claim.setApplicant(employee); claim.setCourseApplication(course); claim.setApprovalManager(reviewer);
        claim.setAmount(course.getCourseFee()); claim.setApplicationStatus(ApplicationStatus.APPLIED); claim.setSubmittedAt(LocalDateTime.now());
        claim.setReceipt(proof.bytes()); claim.setReceiptFileName(proof.name()); claim.setReceiptContentType(proof.type());
        claim.setCertificate(completion.bytes()); claim.setCertificateFileName(completion.name()); claim.setCertificateContentType(completion.type());
        return claims.saveAndFlush(claim);
    }

    // Claim pages and attachments belong to the applicant or the assigned manager.
    public CourseFeeApplication accessible(Integer id, User actor) {
        CourseFeeApplication claim=claims.findById(id).orElseThrow(CourseFeeApplicationService::notFound);
        boolean owner=claim.getApplicant()!=null && actor.getUserId().equals(claim.getApplicant().getUserId());
        boolean reviewer=actor instanceof Manager && claim.getApprovalManager()!=null
                && actor.getUserId().equals(claim.getApprovalManager().getUserId());
        if (!owner && !reviewer) throw notFound();
        return claim;
    }

    // Managers see pending claims assigned to them, not every employee's claim.
    public List<CourseFeeApplication> pending(Integer managerId) {
        return claims.findByApprovalManager_UserIdAndApplicationStatusOrderBySubmittedAtAsc(managerId,ApplicationStatus.APPLIED);
    }

    // Both decisions require a reason; approval leaves the payment date empty.
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void decide(Integer id, Integer managerId, String decision, String reason, Long version) {
        if (decision==null || !Set.of("approve","reject").contains(decision)) throw new IllegalArgumentException("Choose approve or reject.");
        String text=required(reason,"Decision reason",2000);
        Integer employeeId=claims.applicantId(id).orElseThrow(CourseFeeApplicationService::notFound);
        var participants=routing.lockParticipants(employeeId,managerId);
        User manager=participants.get(managerId);
        if (!(manager instanceof Manager) || !manager.isActive()) throw notFound();
        CourseFeeApplication claim=claims.lockById(id).orElseThrow(CourseFeeApplicationService::notFound);
        if (employeeId.equals(managerId) || claim.getApprovalManager()==null || !managerId.equals(claim.getApprovalManager().getUserId())) throw notFound();
        checkVersion(claim,version);
        if (claim.getApplicationStatus()!=ApplicationStatus.APPLIED) throw new IllegalArgumentException("This claim has already been decided.");
        if ("approve".equals(decision) && (claim.getCourseApplication()==null || claim.getCourseApplication().getStatus()!=ApplicationStatus.COMPLETED
                || claim.getCourseApplication().getCourseCategory()==CourseCategoryType.INTERNAL_TRAINING
                || !employeeId.equals(claim.getCourseApplication().getApplicant().getUserId())
                || claim.getReceipt()==null || claim.getCertificate()==null || claim.getAmount().signum()<=0)) {
            throw new IllegalArgumentException("This claim is incomplete or no longer eligible.");
        }
        claim.setApplicationStatus("approve".equals(decision)?ApplicationStatus.APPROVED:ApplicationStatus.REJECTED);
        claim.setReviewer(manager); claim.setReviewedAt(LocalDateTime.now()); claim.setDecisionReason(text); claims.save(claim);
    }

    // Admin records an actual payment separately from the manager's approval.
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void reimburse(Integer id, Integer adminId, String reference, Long version) {
        String text=required(reference,"Payment reference",255);
        Integer employeeId=claims.applicantId(id).orElseThrow(CourseFeeApplicationService::notFound);
        User admin=routing.lockParticipants(employeeId,adminId).get(adminId);
        if (!(admin instanceof Admin) || !admin.isActive()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        CourseFeeApplication claim=claims.lockById(id).orElseThrow(CourseFeeApplicationService::notFound); checkVersion(claim,version);
        if (claim.getApplicationStatus()!=ApplicationStatus.APPROVED || claim.getReimbursedAt()!=null || claim.getAmount().signum()<=0) throw new IllegalArgumentException("Only an approved, unpaid claim with a positive amount can be reimbursed once.");
        claim.setReimbursedAt(LocalDateTime.now()); claim.setReimbursedBy(admin); claim.setPaymentReference(text); claims.save(claim);
    }

    // Keep paid entries in the payment list as an audit trail.
    public List<CourseFeeApplication> approved() { return claims.findByApplicationStatusOrderByReviewedAtAsc(ApplicationStatus.APPROVED); }

    // Claim payments do not spend the annual course budget a second time.
    public BigDecimal reimbursed(User employee, int year) {
        return claims.findByApplicant_UserId(employee.getUserId()).stream().filter(c -> c.getReimbursedAt()!=null
                && c.getCourseApplication()!=null && c.getCourseApplication().getCourseStartDate().getYear()==year)
                .map(CourseFeeApplication::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
    }

    // Filename, declared type and file signature must agree, and each attachment is limited to 5 MB.
    private Upload read(MultipartFile file) {
        if (file==null || file.isEmpty() || file.getSize()>5*1024*1024) throw new IllegalArgumentException("Upload a receipt and certificate, each no larger than 5 MB.");
        try {
            byte[] bytes=file.getBytes();
            String name=file.getOriginalFilename()==null?"document":file.getOriginalFilename().replace('\\','/');
            name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","");
            if (name.length()>255 || name.isBlank()) throw new IllegalArgumentException("Use a document filename of at most 255 characters.");
            String lower=name.toLowerCase(Locale.ROOT),type=file.getContentType();
            boolean pdf=bytes.length>=5 && new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-");
            boolean png=bytes.length>=8 && Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10});
            boolean jpg=bytes.length>=4 && bytes[0]==(byte)255 && bytes[1]==(byte)216 && bytes[2]==(byte)255;
            if (!(pdf && lower.endsWith(".pdf") && "application/pdf".equals(type)
                    || png && lower.endsWith(".png") && "image/png".equals(type)
                    || jpg && (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) && "image/jpeg".equals(type))) {
                throw new IllegalArgumentException("Documents must be PDF, JPEG or PNG files with a matching file type.");
            }
            return new Upload(bytes,name,type);
        } catch (java.io.IOException e) { throw new IllegalArgumentException("Could not read the document. Please upload it again."); }
    }

    // Stale forms reload before a decision or payment can be saved.
    private void checkVersion(CourseFeeApplication claim, Long version) {
        if (version==null || !Objects.equals(version,claim.getVersion())) throw new IllegalArgumentException("This claim changed. Reload it before continuing.");
    }

    // Match user-entered reasons and references to the saved column limits.
    private String required(String value,String label,int length) {
        if (value==null || value.isBlank() || value.trim().length()>length) throw new IllegalArgumentException(label+" is required and cannot exceed "+length+" characters.");
        return value.trim();
    }

    // Missing and inaccessible claims share a 404 response.
    private static ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Claim not found."); }
    private record Upload(byte[] bytes,String name,String type) {}
}

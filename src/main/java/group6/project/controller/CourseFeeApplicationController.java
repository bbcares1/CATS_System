package group6.project.controller;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import group6.project.model.CourseFeeApplication;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.CourseFeeApplicationRepo;
import group6.project.service.CourseFeeApplicationService;
@Controller 
public class CourseFeeApplicationController {

    private final CourseFeeApplicationService courseFeeApplicationService;

    public CourseFeeApplicationController(CourseFeeApplicationService courseFeeApplicationService, CourseFeeApplicationRepo courseFeeApplicationRepo, CourseApplicationRepo courseApplicationRepo) {
        this.courseFeeApplicationService = courseFeeApplicationService;
    }

    //Show application list
    @GetMapping("/course-fee-applications")
    public String getAllApplications(Model model) {
        model.addAttribute("feeApplications", courseFeeApplicationService.getAllApplications());
        return "course-fee-applications-list";
    }

    //View single claim
    @GetMapping({"/course-fee-applications/{id}", "/course-fee-applicaitions/{id}"})
    public String getFeeApplication(@PathVariable ("id") Integer applicationId, Model model){
        Optional<CourseFeeApplication> existingApplication = courseFeeApplicationService.getApplicationById(applicationId);

        if(existingApplication.isPresent()){
            CourseFeeApplication application = existingApplication.get();
            model.addAttribute("feeApplication", application);
            return "single-fee-application";        
        }
        throw new RuntimeException(
            "Course fee application not found"
        );

    }

    //Submit claim form
    @GetMapping({"/course-fee-applications/new", "/course-fee-applicaitions/new"})
    public String showSubmitForm(Model model){
        CourseFeeApplication courseFeeApplication = new CourseFeeApplication();
        model.addAttribute("feeApplication", courseFeeApplication);
        return "course-fee-application-form";
    }

    //Save claimed form
    @PostMapping("/course-fee-applications")
    public String sendApplication(
            @ModelAttribute CourseFeeApplication courseFeeApplication,
            @RequestParam("receiptFile") MultipartFile receiptFile,
            @RequestParam("certificateFile") MultipartFile certificateFile)
            throws IOException {

        courseFeeApplication.setReceipt(
            receiptFile.getBytes()
        );

        courseFeeApplication.setReceiptFileName(
            receiptFile.getOriginalFilename()
        );

        courseFeeApplication.setReceiptContentType(
            receiptFile.getContentType()
        );

        courseFeeApplication.setCertificate(
            certificateFile.getBytes()
        );

        courseFeeApplication.setCertificateFileName(
            certificateFile.getOriginalFilename()
        );

        courseFeeApplication.setCertificateContentType(
            certificateFile.getContentType()
        );

        courseFeeApplicationService
            .submitApplication(courseFeeApplication);

        return "redirect:/course-fee-applications";
    }

    //Manager approval or rejection
    @PostMapping("/course-fee-applications/{id}/decision")
    public String processDecision(
            @PathVariable("id") Integer applicationId,
            @RequestParam String decision,
            @RequestParam String reason) {
                if ("approve".equals(decision)) {
                    courseFeeApplicationService
                        .approveFeeApplication(applicationId, reason);

                } else if ("reject".equals(decision)) {
                    courseFeeApplicationService
                        .rejectFeeApplication(applicationId, reason);
                }
                return "redirect:/course-fee-applications";
    }
    
    //View receipt
    @GetMapping("/course-fee-applications/{id}/receipt")
    public ResponseEntity<byte[]> viewReceipt(
            @PathVariable("id") Integer applicationId) {
                Optional <CourseFeeApplication> existingApplication = courseFeeApplicationService.getApplicationById(applicationId);
                if (existingApplication.isPresent()){
                    CourseFeeApplication application = existingApplication.get();
                    return ResponseEntity.ok()
                                    .header(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        "inline; filename=\"" +
                                        application.getReceiptFileName() +
                                        "\""
                                    )
                                    .contentType(
                                        MediaType.parseMediaType(
                                            application.getReceiptContentType()
                                        )
                                    )
                                    .body(application.getReceipt());
                        }

                        throw new RuntimeException(
                            "Course fee application not found"
                        );
                }
            
    //View certificate
    @GetMapping("/course-fee-applications/{id}/certificate")
    public ResponseEntity<byte[]> viewCertificate(
            @PathVariable("id") Integer applicationId) {
                Optional <CourseFeeApplication> existingApplication = courseFeeApplicationService.getApplicationById(applicationId);
                if (existingApplication.isPresent()){
                    CourseFeeApplication application = existingApplication.get();
                    return ResponseEntity.ok()
                                    .header(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        "inline; filename=\"" +
                                        application.getCertificateFileName() +
                                        "\""
                                    )
                                    .contentType(
                                        MediaType.parseMediaType(
                                            application.getCertificateContentType()
                                        )
                                    )
                                    .body(application.getCertificate());
                        }

                        throw new RuntimeException(
                            "Course fee application not found"
                        );
                }    
                
}

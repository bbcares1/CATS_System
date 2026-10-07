package group6.project.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import group6.project.model.ApplicationStatus;
import group6.project.model.CourseFeeApplication;
import group6.project.repo.CourseFeeApplicationRepo;

@Service
public class CourseFeeApplicationService {
  private final CourseFeeApplicationRepo courseFeeApplicationRepo;
  public CourseFeeApplicationService(CourseFeeApplicationRepo courseFeeApplicationRepo){
    this.courseFeeApplicationRepo = courseFeeApplicationRepo;
  }

  public List<CourseFeeApplication> getAllApplications(){
    return courseFeeApplicationRepo.findAll();
  }

  public Optional<CourseFeeApplication> getApplicationById(Integer applicationId){
    return courseFeeApplicationRepo.findById(applicationId);
  }

  public CourseFeeApplication submitApplication(CourseFeeApplication application){
    application.setApplicationStatus(ApplicationStatus.APPLIED);
    application.setSubmittedAt(LocalDateTime.now());
    return courseFeeApplicationRepo.save(application);
  }

  public CourseFeeApplication approveFeeApplication(Integer applicationId, String reason){
    //Reason validation
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException(
          "Decision reason is required"
      );
    }

    //Check the database
    Optional<CourseFeeApplication> existingApplication =
        courseFeeApplicationRepo.findById(applicationId); 
    if (existingApplication.isPresent()){
      CourseFeeApplication existing = existingApplication.get();
      existing.setApplicationStatus(ApplicationStatus.APPROVED);
      existing.setReviewedAt(LocalDateTime.now());
      existing.setDecisionReason(reason);
      return courseFeeApplicationRepo.save(existing);
    }

    throw new RuntimeException("Course fee application not found");
  }

  public CourseFeeApplication rejectFeeApplication(Integer applicationId, String reason){
    //Reason validation
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException(
        "Decision reason is required");
    }

    //Then check database
    Optional<CourseFeeApplication> existingApplication =
        courseFeeApplicationRepo.findById(applicationId); 
    if (existingApplication.isPresent()){
      CourseFeeApplication existing = existingApplication.get();
      existing.setApplicationStatus(ApplicationStatus.REJECTED);
      existing.setReviewedAt(LocalDateTime.now());
      existing.setDecisionReason(reason);
      return courseFeeApplicationRepo.save(existing);
    }

    throw new RuntimeException("Course fee application not found");
  }

}

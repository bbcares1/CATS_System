package group6.project.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import group6.project.model.ApprovalHierarchy;
import group6.project.model.CourseCategory;
import group6.project.model.CourseDetail;
import group6.project.model.ExcludedDays;
import group6.project.model.Manager;
import group6.project.model.Roles;
import group6.project.model.Staff;
import group6.project.model.TrainingEntitlement;
import group6.project.repo.AdminRepo;
import group6.project.repo.ApprovalHierarchyRepo;
import group6.project.repo.CourseDetailRepo;
import group6.project.repo.ExcludedDaysRepo;
import group6.project.repo.ManagerRepo;
import group6.project.repo.StaffRepo;
import group6.project.repo.TrainingEntitlementRepo;
import jakarta.transaction.Transactional;

import group6.project.model.User;
import group6.project.model.Admin;
import group6.project.model.Manager;
import group6.project.repo.UserRepo;

@Service
public class AdminService {
     @Autowired
     public AdminRepo adminRepo;

     @Autowired
     public StaffRepo staffRepo;

     @Autowired
     public TrainingEntitlementRepo trainingEntitlementRepo;

     @Autowired
     public ExcludedDaysRepo excludedDaysRepo;

     @Autowired
     private UserRepo userRepo;

     @Autowired
     public CourseDetailRepo courseDetailRepo;

     @Autowired
     public ApprovalHierarchyRepo approvalHierarchyRepo;

     @Autowired
     public ManagerRepo managerRepo;

     @Transactional
     public void updateStaffBudget(Integer Id, Double new_budget, Integer new_days) {
          if (new_budget == null || new_budget < 0 || new_days == null || new_days < 0) {
               throw new IllegalArgumentException(
                         "Training budget and days must be zero or greater");
          }

          Optional<Staff> targeted_staff = staffRepo.findById(Id);
          if (targeted_staff.isEmpty()) {
               throw new RuntimeException("can not find ID as " + Id + " staff");
          } else {
               Staff staff = targeted_staff.get();
               staff.setTrainingBudget(new_budget);
               staff.setTrainingDays(new_days);

               staffRepo.save(staff);
          }

     }

     public List<Staff> getAllStaff() {
          return staffRepo.findAll();
     }

     public Optional<Staff> getIdStaff(Integer id) {
          return staffRepo.findById(id);
     }

     public List<Admin> getAllAdmins() {
          return adminRepo.findAll();
     }









     public List<Manager> getManagerList() {
          return managerRepo.findAll();
     }

     // here below is about excluded days
     // -----------------------------------

     public List<ExcludedDays> getAllExcludedDays() {
          return excludedDaysRepo.findAll();
     }

     public void saveExcludedDays(ExcludedDays excludedDays) {
          excludedDaysRepo.save(excludedDays);
     }

     public void deleteExcludedDays(Integer id) {
          excludedDaysRepo.deleteById(id);
     }

     public List<CourseDetail> getAllCourseDetails() {

          return courseDetailRepo.findAll();
     }

     public Optional<CourseDetail> getByIdCourseDetails(Integer id) {
          return courseDetailRepo.findById(id);
     }

     public void saveCourse(CourseDetail course) {

          courseDetailRepo.save(course);
     }

     public void deleteCourseById(Integer id) {
          courseDetailRepo.deleteById(id);
     }

     public List<ApprovalHierarchy> getAllApprovalHierarchy() {
          return approvalHierarchyRepo.findAllByOrderByLevelAsc();
     }

     public Optional<ApprovalHierarchy> getHierarchyById(Integer id) {
          return approvalHierarchyRepo.findById(id);
     }

     public void saveHierarchy(ApprovalHierarchy hierarchy) {
          approvalHierarchyRepo.save(hierarchy);
     }

     public void deleteHierarchyById(Integer id) {
          approvalHierarchyRepo.deleteById(id);
     }

     public void save(Staff staff) {
          // TODO Auto-generated method stub
          throw new UnsupportedOperationException("Unimplemented method 'save'");
     }

     public List<User> viewList() {
          return userRepo.findAll();
     }


}

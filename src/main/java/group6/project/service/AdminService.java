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
     public CourseDetailRepo courseDetailRepo;

     @Autowired
     public ApprovalHierarchyRepo approvalHierarchyRepo;

     @Autowired
     public ManagerRepo managerRepo;

     @Transactional
     public void updateStaffBudget(Integer Id, Double new_budget, Integer new_days) {

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

     public void saveStaff(Staff staff) {
          if (staff.getManager() != null && staff.getManager().getUserId() == null) {
               staff.setManager(null);
          } 
          staffRepo.save(staff);
     }

     public void deleteStaffById(Integer id) {
          staffRepo.deleteById(id);
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

     // here below is about approvalhierarchy
     // -------------------------------------

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

}

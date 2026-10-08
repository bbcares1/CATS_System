package group6.project.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import group6.project.model.ApprovalHierarchy;
import group6.project.model.CourseCategory;
import group6.project.model.CourseDetail;
import group6.project.model.ExcludedDays;
import group6.project.model.Staff;
import group6.project.model.TrainingEntitlement;
import group6.project.repo.AdminRepo;
import group6.project.repo.ApprovalHierarchyRepo;
import group6.project.repo.CourseDetailRepo;
import group6.project.repo.ExcludedDaysRepo;
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

     @Transactional
     public void updateStaffBudget(Integer Id, Double new_budget, Integer new_days) {

          Optional<Staff> targeted_staff = staffRepo.findById(Id);
          if (targeted_staff.isEmpty()) {
               throw new RuntimeException("未找到 ID 为 " + Id + " 的员工");
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

     public void save(Staff staff) {
          staffRepo.save(staff);
     }

     public void deleteById(Integer id) {
          staffRepo.deleteById(id);
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

     
    
    public List<User> viewList() {
        return userRepo.findAll();
    }

    
    @Transactional
    public User createAccount(
            String userName,
            String name,
            String designation,
            String accountType,
            String staffNo,
            String password) {

        validateAccountFields(userName, name);

        String username = userName.trim();

        if (userRepo.findByUserName(username).isPresent()) {
            throw new IllegalArgumentException(
                "Username already exists");
        }

        User user;

        switch (accountType) {
            case "Admin":
                Admin admin = new Admin();
                admin.setStaffNo(staffNo);
                user = admin;
                break;

            case "Manager":
                Manager manager = new Manager();
                manager.setStaffNo(staffNo);
                user = manager;
                break;

            case "User":
                user = new User();
                break;

            default:
                throw new IllegalArgumentException(
                    "Invalid account type");
        }

        user.setUserName(username);
        user.setName(name.trim());
        user.setDesignation(designation);

        user.setPassword(password);

        return userRepo.save(user);
    }

    
    @Transactional
    public void deleteAccount(Integer userId) {

        User user = userRepo.findById(userId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Account not found: " + userId));

        userRepo.delete(user);
        userRepo.flush();
    }

    
    @Transactional
    public User updateAccount(
            Integer userId,
            String userName,
            String name,
            String designation) {

        validateAccountFields(userName, name);

        User existingUser = userRepo.findById(userId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Account not found: " + userId));

        String username = userName.trim();

        Optional<User> duplicate =
            userRepo.findByUserName(username);

        if (duplicate.isPresent()
                && !duplicate.get().getUserId()
                    .equals(userId)) {
            throw new IllegalArgumentException(
                "Username already exists");
        }

        existingUser.setUserName(username);
        existingUser.setName(name.trim());
        existingUser.setDesignation(designation);

        return userRepo.save(existingUser);
    }

    private void validateAccountFields(
            String userName, String name) {

        if (userName == null ||
                userName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Username cannot be empty");
        }

        if (name == null ||
                name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Name cannot be empty");
        }
    }

}

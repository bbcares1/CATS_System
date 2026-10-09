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
import group6.project.model.AccountForm;
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

     @Transactional
     public void saveStaff(Staff form) { // this function is use to edit existed staff i separate create and edit into
                                         // two different method
          Staff target;

          if (form.getUserId() == null) {

               if (form.getRole() == Roles.MANAGER) {
                    target = new Manager();
               } else if (form.getRole() == Roles.STAFF) {
                    target = new Staff();
               } else {
                    throw new IllegalArgumentException(
                              "Only staff and manager records can be saved here");
               }
          } else {
               Optional<Staff> optionalStaff = staffRepo.findById(form.getUserId());

               if (optionalStaff.isPresent()) {

                    target = optionalStaff.get();
               } else {

                    throw new IllegalArgumentException("Staff member not found: " + form.getUserId());
               }
          }

          target.setName(form.getName());
          target.setUserName(form.getUserName());
          target.setDesignation(form.getDesignation());
          target.setStaffId(form.getStaffId());
          target.setTrainingBudget(form.getTrainingBudget());
          target.setTrainingDays(form.getTrainingDays());
          target.setRole(form.getRole());
          if (form.getPassword() != null && !form.getPassword().isBlank()) {
               target.setPassword(form.getPassword());
          }

          if (form.getManager() != null && form.getManager().getUserId() != null) {

               Integer managerId = form.getManager().getUserId();

               Manager managerObj = managerRepo.findById(managerId).orElse(null);
               target.setManager(managerObj);
          } else {

               target.setManager(null);
          }

          staffRepo.save(target);

     }

     @Transactional // this is used to create a new staff
     public User createAccount(AccountForm form) {
          if (form.getRole() == null) {
               throw new IllegalArgumentException("Please select an account role");
          }

          String username = form.getUserName().trim();
          String name = form.getName().trim();

          if (userRepo.findByUserName(username).isPresent()) {
               throw new IllegalArgumentException("Username already exists");
          }
          if (form.getEmail() == null || form.getEmail().isBlank()) {
               throw new IllegalArgumentException("Email is required");
          }
          String email = form.getEmail().trim();
          if (userRepo.findByEmail(email).isPresent()) {
               throw new IllegalArgumentException("Email address already belongs to an account");
          }

          if (form.getRole() != Roles.ADMIN
                    && (form.getStaffId() == null
                              || form.getStaffId().isBlank()
                              || form.getTrainingBudget() == null
                              || form.getTrainingBudget() < 0
                              || form.getTrainingDays() == null
                              || form.getTrainingDays() < 0)) {
               throw new IllegalArgumentException(
                         "Staff ID is required and training budget and days must be zero or greater");
          }

          User account;
          switch (form.getRole()) {
               case ADMIN -> {
                    Admin admin = new Admin();
                    admin.setStaffId(form.getStaffId());
                    account = admin;
               }
               case MANAGER -> {
                    Manager manager = new Manager();
                    setEmployeeFields(manager, form);
                    account = manager;
               }
               case STAFF -> {
                    Staff staff = new Staff();
                    setEmployeeFields(staff, form);
                    account = staff;
               }
               default -> throw new IllegalArgumentException(
                         "Please select a valid account role");
          }

          account.setUserName(username);
          account.setName(name);
          account.setEmail(email);
          account.setDesignation(form.getDesignation());
          account.setPassword(form.getPassword());
          account.setRole(form.getRole());

          return userRepo.save(account);
     }

     private void setEmployeeFields(Staff employee, AccountForm form) {
          employee.setStaffId(form.getStaffId());
          employee.setTrainingBudget(form.getTrainingBudget());
          employee.setTrainingDays(form.getTrainingDays());

          if (form.getManagerId() != null) {
               Manager manager = managerRepo.findById(form.getManagerId())
                         .orElseThrow(() -> new IllegalArgumentException(
                                   "Selected manager was not found"));
               employee.setManager(manager);
          }
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

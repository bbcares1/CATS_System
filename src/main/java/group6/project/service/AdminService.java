package group6.project.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import group6.project.model.Staff;
import group6.project.model.TrainingEntitlement;
import group6.project.repo.AdminRepo;
import group6.project.repo.StaffRepo;
import group6.project.repo.TrainingEntitlementRepo;
import jakarta.transaction.Transactional;

public class AdminService {
     @Autowired 
     public AdminRepo adminRepo;

     @Autowired 
     public StaffRepo staffRepo;

     @Autowired 
     public TrainingEntitlementRepo trainingEntitlementRepo;



     @Transactional
     public void updateStaffBudget(Integer Id, Double new_budget, Integer new_days){
               
          Optional<Staff> targeted_staff = staffRepo.findById(Id);
          if(targeted_staff.isEmpty()){
           throw new RuntimeException("未找到 ID 为 " + Id + " 的员工");
          }
          else{
             Staff staff = targeted_staff.get();
             staff.setTrainingBudget(new_budget);
             staff.setTrainingDays(new_days);

             staffRepo.save(staff);
          }

     }

     public List<Staff> getAllStaff(){
           return staffRepo.findAll();
     }

     
}

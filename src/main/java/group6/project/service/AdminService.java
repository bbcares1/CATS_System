package group6.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import group6.project.repo.AdminRepo;
import group6.project.repo.UserRepository;
import group6.project.model.User;
import group6.project.model.CourseApplication;
import group6.project.model.ApprovalHierarchy;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.ApprovalHierarchyRepo;
import java.util.List;
import java.util.Optional;

@Service
public class AdminService {
     @Autowired 
     public AdminRepo adminRepo;
     @Autowired
     private UserRepository userRepository;
     @Autowired
     private CourseApplicationRepo courseApplicationRepo;
     @Autowired
     private ApprovalHierarchyRepo approvalHierarchyRepo;

     public List<User> viewList(){
          return userRepository.findAll();
     }

     public User createAccount(User user){
          return userRepository.save(user);
     }

     public void deleteAccount(Integer userId){
          Optional<User> opt = userRepository.findById(userId);
          if(opt.isPresent()){
               userRepository.deleteById(userId);
          }
     }

     public User updateAccount(User user){
          return userRepository.save(user);
     }

     public CourseApplication courseApplicationCreate(CourseApplication courseApplication){
        return courseApplicationRepo.save(courseApplication);
     }

    public void courseFeeReimbursementApprovement(Integer feeApplicationId){
        
    }

    public ApprovalHierarchy manageHierarchySave(ApprovalHierarchy entity){
        return approvalHierarchyRepo.save(entity);
    }
    public List<ApprovalHierarchy> manageHierarchyFindAll(){
        return approvalHierarchyRepo.findAll();
    }
    public void manageHierarchyDelete(Integer id){
        approvalHierarchyRepo.deleteById(id);
    }
    public Optional<ApprovalHierarchy> manageHierarchyFindById(Integer id){
        return approvalHierarchyRepo.findById(id);
    }
}

package group6.project.controller;

import group6.project.service.AdminService;
import group6.project.model.User;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import group6.project.model.CourseApplication;
import group6.project.model.ApprovalHierarchy;


@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService){
         this.adminService = adminService;
    }

    @GetMapping("/accounts")
    public List<User> viewList(){
        return adminService.viewList();
    }

    @PostMapping("/accounts")
    public User createAccount(@RequestBody User user){
        return adminService.createAccount(user);
    }

    @DeleteMapping("/accounts/{userId}")
    public void deleteAccount(@PathVariable Integer userId){
        adminService.deleteAccount(userId);
    }

    @PutMapping("/accounts")
    public User updateAccount(@RequestBody User user){
        return adminService.updateAccount(user);
    }

    @PostMapping("/course‑application")
    public ResponseEntity<CourseApplication> courseApplicationCreate(@RequestBody CourseApplication courseApplication){
        CourseApplication res = adminService.courseApplicationCreate(courseApplication);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @PutMapping("/fee‑reimbursement/approve/{id}")
    public ResponseEntity<Void> courseFeeReimbursementApprovement(@PathVariable Integer id){
        adminService.courseFeeReimbursementApprovement(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hierarchy")
    public ResponseEntity<ApprovalHierarchy> addHierarchy(@RequestBody ApprovalHierarchy ApprovalHierarchy){
        ApprovalHierarchy saved = adminService.manageHierarchySave(ApprovalHierarchy);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/hierarchy")
    public ResponseEntity<List<ApprovalHierarchy>> getAllHierarchy(){
        List<ApprovalHierarchy> list = adminService.manageHierarchyFindAll();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/hierarchy/{id}")
    public ResponseEntity<Void> deleteHierarchy(@PathVariable Integer id){
        adminService.manageHierarchyDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/hierarchy/{id}")
    public ResponseEntity<ApprovalHierarchy> getHierarchyById(@PathVariable Integer id){
        Optional<ApprovalHierarchy> opt = adminService.manageHierarchyFindById(id);
        return opt.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

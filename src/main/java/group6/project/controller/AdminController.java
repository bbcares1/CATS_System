package group6.project.controller;

import group6.project.service.AdminService;
import group6.project.model.User;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Void> courseFeeReimbursementApprove(@PathVariable Integer id){
        adminService.courseFeeReimbursementApprove(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hierarchy")
    public ResponseEntity<ApprovalHierarchy> addHierachy(@RequestBody ApprovalHierarchy ApprovalHierarchy){
        ApprovalHierarchy saved = adminService.manageHierachySave(ApprovalHierarchy);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/hierarchy")
    public ResponseEntity<List<ApprovalHierarchy>> getAllHierachy(){
        List<ApprovalHierarchy> list = adminService.manageHierachyFindAll();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/hierarchy/{id}")
    public ResponseEntity<Void> deleteHierachy(@PathVariable Integer id){
        adminService.manageHierachyDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/hierarchy/{id}")
    public ResponseEntity<ApprovalHierarchy> getHierachyById(@PathVariable Integer id){
        Optional<ApprovalHierarchy> opt = adminService.manageHierachyFindById(id);
        return opt.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

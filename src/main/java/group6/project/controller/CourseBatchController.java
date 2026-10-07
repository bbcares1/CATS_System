package group6.project.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import group6.project.model.CourseBatch;
import group6.project.service.AdminService;
import group6.project.service.CourseBatchService;
import group6.project.model.CourseDetail;




@Controller
@RequestMapping ("/admin/batches")
public class CourseBatchController {

    private final CourseBatchService courseBatchService;
    private final AdminService adminService;

    public CourseBatchController(CourseBatchService courseBatchService, AdminService adminService) {
        this.courseBatchService = courseBatchService;
        this.adminService = adminService;
    }

    @GetMapping
    public String getAllBatches(Model model) {
        model.addAttribute("batches", courseBatchService.getAllBatches());
        return "course-batch-list";
    }

    @GetMapping("/{id}")
    public String getBatch(@PathVariable ("id") Long batchId, Model model) {
        Optional <CourseBatch> existingBatch = courseBatchService.getBatchById(batchId);
        if (existingBatch.isPresent()){
            CourseBatch batch = existingBatch.get();
            model.addAttribute("singleBatch", batch);
            return "course-single-batch";
        }
        throw new RuntimeException("Course batch not found");
    }

    @GetMapping("/new")
    public String showBatchForm(Model model) {

        model.addAttribute(
            "newBatch",
            new CourseBatch()
        );

        model.addAttribute(
            "courses",
            adminService.getAllCourseDetails()
        );

        return "course-batch-form";
    }
    

    @PostMapping("/new")
    public String sendBatch(
            @ModelAttribute CourseBatch courseBatch,
            @RequestParam Integer courseId) {

        Optional<CourseDetail> selectedCourse =
                adminService.getByIdCourseDetails(courseId);

        if (selectedCourse.isPresent()) {

            CourseDetail course =
                    selectedCourse.get();

            courseBatch.setCourseDetail(course);

            courseBatchService.createBatch(courseBatch);

            return "redirect:/admin/batches";
        }

        throw new RuntimeException("Course not found");
    }

    @GetMapping("/delete")
    public String showDeleteForm(Model model){
        model.addAttribute("batches", courseBatchService.getAllBatches());

        return "course-batch-delete";
    }

    @PostMapping("/delete")
    public String deleteBatch(@RequestParam Long batchId){
        courseBatchService.deleteBatch(batchId);
        return "redirect:/admin/batches";
    }

    @GetMapping("/edit/{id}")
    public String showEditBatchForm(@PathVariable("id") Long batchId, Model model) {
        Optional<CourseBatch> existingBatch = courseBatchService.getBatchById(batchId);
        if (existingBatch.isPresent()){
        CourseBatch batch = existingBatch.get();
        model.addAttribute(
            "courseBatch", batch
        );

        return "course-batch-edit";
        }

        return "redirect:/admin/batches";
    }

    @PostMapping("/edit/{id}")
    public String updateBatch(@PathVariable ("id") Long batchId, @ModelAttribute CourseBatch courseBatch) {
        courseBatchService.updateBatch(courseBatch, batchId);
        return "redirect:/admin/batches";
    }
}

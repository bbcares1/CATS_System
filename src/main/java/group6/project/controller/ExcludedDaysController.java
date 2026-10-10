package group6.project.controller;
import org.springframework.validation.BindingResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import group6.project.model.ExcludedDays;
import group6.project.service.ExcludedDaysService;

@Controller
@RequestMapping("/excluded-days")
public class ExcludedDaysController {

    private final ExcludedDaysService excludedDaysService;

    public ExcludedDaysController(ExcludedDaysService excludedDaysService) {
        this.excludedDaysService = excludedDaysService;
    }

    @GetMapping
    public String getAllExcludedDays(Model model) {
        populateListModel(model);
        return "ExcludedDaysList";
    }

    @GetMapping("/add")
    public String showAddForm() {
        return "redirect:/excluded-days";
    }

    @PostMapping("/add")
    public String addExcludedDay(
            @Valid @ModelAttribute("newExcludedDay") ExcludedDays excludedDay,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            populateListModel(model);
            return "ExcludedDaysList";
        }
        excludedDaysService.addExcludedDay(excludedDay);
        redirectAttributes.addFlashAttribute("success", "Public holiday added successfully");
        return "redirect:/excluded-days";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Integer id,
            Model model) {
        ExcludedDays excludedDay =
                excludedDaysService.getExcludedDayById(id);
        model.addAttribute("excludedDay", excludedDay);
        return "ExcludedDaysEdit";
    }

    @PostMapping("/edit/{id}")
    public String updateExcludedDay(
            @PathVariable Integer id,
            @Valid @ModelAttribute("excludedDay") ExcludedDays excludedDay,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            excludedDay.setId(id);
            return "ExcludedDaysEdit";
        }
        excludedDaysService.updateExcludedDay(id, excludedDay);
        redirectAttributes.addFlashAttribute("success", "Public holiday updated successfully");
        return "redirect:/excluded-days";
    }

    @PostMapping("/delete/{id}")
    public String deleteExcludedDay(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        excludedDaysService.deleteExcludedDay(id);
        redirectAttributes.addFlashAttribute("success", "Public holiday deleted successfully");
        return "redirect:/excluded-days";
    }

    private void populateListModel(Model model) {
        model.addAttribute(
                "excludedDaysList",
                excludedDaysService.getAllExcludedDays());
        if (!model.containsAttribute("newExcludedDay")) {
            model.addAttribute("newExcludedDay", new ExcludedDays());
        }
    }
}
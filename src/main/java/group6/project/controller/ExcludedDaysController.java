package group6.project.controller;
import org.springframework.validation.BindingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;
import group6.project.service.ExcludedDaysService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import group6.project.model.ExcludedDays;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/excluded-days")
public class ExcludedDaysController{

    @Autowired
    private ExcludedDaysService excludedDaysService;
    @GetMapping
    public String getAllExcludedDays(Model model) {
        model.addAttribute(
            "excludedDays",
            excludedDaysService.getAllExcludedDays()
        );
        return "excluded-days";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
       model.addAttribute(
        "excludedDay", new ExcludedDays()
    );
    return "excluded-days-add";
}

    @PostMapping("/add")
    public String addExcludedDay(
            @Valid @ModelAttribute("excludedDay") ExcludedDays excludedDay,BindingResult result) {
        if (result.hasErrors()) {
            return "excluded-days-add";
    }
        excludedDaysService.addExcludedDay(excludedDay);
        return "redirect:/excluded-days";
}

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Integer id,
            Model model) {
       ExcludedDays excludedDay =
               excludedDaysService.getExcludedDayById(id);
       model.addAttribute("excludedDay", excludedDay);
       return "excluded-days-edit";
}

    @PostMapping("/edit/{id}")
    public String updateExcludedDay(
            @PathVariable Integer id,
            @Valid @ModelAttribute("excludedDay") ExcludedDays excludedDay,BindingResult result) {
        if (result.hasErrors()) {
        return "excluded-days-edit";
    }
        excludedDaysService.updateExcludedDay(id, excludedDay);
        return "redirect:/excluded-days";
}

    @PostMapping("/delete/{id}")
    public String deleteExcludedDay(@PathVariable Integer id) {
        excludedDaysService.deleteExcludedDay(id);
        return "redirect:/excluded-days";
}
}
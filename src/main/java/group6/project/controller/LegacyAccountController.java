package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class LegacyAccountController {
    // Existing dashboard and email links keep reaching the unified account list.
    @GetMapping("/admin/staffs")
    public String accounts() {
        return "redirect:/admin/accounts";
    }

    // Old create links use the same small account form.
    @GetMapping("/admin/staffs/add")
    public String create() {
        return "redirect:/admin/accounts/new";
    }

    // Keep bookmarks while replacing entity binding with a manager ID field.
    @GetMapping("/admin/staffs/update/{id}")
    public String edit(@PathVariable Integer id) {
        return "redirect:/admin/accounts/" + id + "/edit";
    }
}

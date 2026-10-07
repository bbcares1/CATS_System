package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import group6.project.model.Manager;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/manager")
public class ManagerController {

    @GetMapping({"", "/home"})
    public String managerHome(HttpSession session, Model model) {
        if (!(session.getAttribute("user") instanceof Manager manager)) {
            return "redirect:/employee/login";
        }

        model.addAttribute("currentUser", manager);
        return "manager-home";
    }
}

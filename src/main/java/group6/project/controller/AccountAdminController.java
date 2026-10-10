// We handle account forms and reporting-manager choices here.
package group6.project.controller;

import group6.project.form.AccountForm;
import group6.project.model.Admin;
import group6.project.model.Roles;
import group6.project.service.AccountAdminService;
import group6.project.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/accounts")
public class AccountAdminController {
    private final AccountAdminService accounts;
    private final UserService users;

    public AccountAdminController(AccountAdminService accounts, UserService users) {
        this.accounts = accounts;
        this.users = users;
    }

    // Staff, Manager and Admin are managed from one account list.
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        var result = PageSupport.page(accounts.all(), page, size);
        model.addAttribute("accounts", result.getContent());
        model.addAttribute("pageData", result);
        return "admin-account-list";
    }

    // Account limits are allocated separately per year, after creating employee identity.
    @GetMapping("/new")
    public String create(Model model) {
        return form(null, new AccountForm(), model);
    }

    // Do not populate a stored password into the edit page or its HTML.
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Model model) {
        return form(id, accounts.form(accounts.get(id)), model);
    }

    // Validation errors retain the form except for its password field.
    @PostMapping("/save")
    public String save(
            @RequestParam(required = false) Integer id,
            @Valid @ModelAttribute("form") AccountForm form,
            BindingResult binding,
            HttpSession session,
            Model model,
            RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            form.setPassword(null);
            return form(id, form, model);
        }
        try {
            accounts.save(id, form, actorId(session));
            redirect.addFlashAttribute("success", "Account saved.");
            return "redirect:/admin/accounts";
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            binding.reject("account", error.getReason());
            form.setPassword(null);
            return form(id, form, model);
        } catch (DataIntegrityViolationException error) {
            binding.reject("account", "Username, Staff ID or email already exists.");
            form.setPassword(null);
            return form(id, form, model);
        }
    }

    // A confirmed POST can delete only an unused account; history is never cascaded away.
    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Integer id,
            @RequestParam Long version,
            HttpSession session,
            RedirectAttributes redirect) {
        try {
            accounts.delete(id, version, actorId(session));
            redirect.addFlashAttribute("success", "Account deleted.");
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            redirect.addFlashAttribute("error", error.getReason());
        }
        return "redirect:/admin/accounts";
    }

    // A role/session change between the interceptor and this handler must fail as 403, not a
    // null-pointer error.
    private Integer actorId(HttpSession session) {
        var actor = users.currentUser(session);
        if (!(actor instanceof Admin)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return actor.getUserId();
    }

    // Populate shared dropdowns after both GETs and rejected submissions.
    private String form(Integer id, AccountForm form, Model model) {
        model.addAttribute("accountId", id);
        model.addAttribute("form", form);
        model.addAttribute("roles", Roles.values());
        model.addAttribute("managers", accounts.managers());
        return "admin-account-form";
    }
}

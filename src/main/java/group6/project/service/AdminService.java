package group6.project.service;

import group6.project.model.User;
import group6.project.repo.UserRepo;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminService {
    private final UserRepo users;

    // Account and catalogue writes remain in their own services.
    public AdminService(UserRepo users) {
        this.users = users;
    }

    // Reuse saved accounts for the reporting chart and manual email recipients.
    public List<User> viewList() {
        return users.findAll(Sort.by("name"));
    }
}

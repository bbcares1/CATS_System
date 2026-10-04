package group6.project.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import group6.project.model.Roles;
import group6.project.model.User;
import group6.project.repo.UserRepo;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    public User authenticate(String username, String password) {
        Optional<User> optionalUser = userRepo.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            
            if (user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    public boolean isAdmin(User user) {
        return user != null && user.getRole() == Roles.ADMIN;
    }

    public boolean isStaffOrManager(User user) {
        return user != null && (user.getRole() == Roles.STAFF || user.getRole() == Roles.MANAGER);
    }
}
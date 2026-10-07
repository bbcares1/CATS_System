package group6.project.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import group6.project.model.Admin;
import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.repo.UserRepo;

@Service 
public class UserService {

    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public User authenticate(String userName,String password) {
        Optional<User> userOptional = userRepo.findByUserName(userName);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }


    public boolean isAdmin(User user) {
        return user instanceof Admin;
    }


    public boolean isManager(User user) {
        return user instanceof Manager;
    }


    public boolean isStaff(User user) {
        return user instanceof Staff && !(user instanceof Manager);
    }


    public boolean isStaffOrManager(User user) {
        return user instanceof Staff;
    }
}

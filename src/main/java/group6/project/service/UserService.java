package group6.project.service;

import org.springframework.beans.factory.annotation.Autowired;

import group6.project.repo.UserRepo;

public class UserService {
    @Autowired 
    private UserRepo usrepo;
    
}

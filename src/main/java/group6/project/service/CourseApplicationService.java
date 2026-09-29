package group6.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import group6.project.repo.CourseApplicationRepo;

@Service
public class CourseApplicationService {

    @Autowired 
    public CourseApplicationRepo courseApplicationRepo;
}

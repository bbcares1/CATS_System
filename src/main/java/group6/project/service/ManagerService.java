package group6.project.service;

import org.springframework.stereotype.Service;

import group6.project.repo.ManagerRepo;

@Service 
public class ManagerService {

  private final ManagerRepo managerRepo;

  public ManagerService(ManagerRepo managerRepo) {
    this.managerRepo = managerRepo;
  }

}

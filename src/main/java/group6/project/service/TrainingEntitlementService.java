package group6.project.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import group6.project.model.TrainingEntitlement;
import group6.project.repo.TrainingEntitlementRepo;

@Service
public class TrainingEntitlementService {

    private final TrainingEntitlementRepo trainingEntitlementRepo;

    public TrainingEntitlementService(
            TrainingEntitlementRepo trainingEntitlementRepo) {
        this.trainingEntitlementRepo = trainingEntitlementRepo;
    }

    public List<TrainingEntitlement> getAllTrainingEntitlements() {
        return trainingEntitlementRepo.findAll();
    }

    public TrainingEntitlement getTrainingEntitlement(Integer id) {
        return trainingEntitlementRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Training entitlement not found with id: " + id));
    }

    public List<TrainingEntitlement> getEntitlementsByStaff(Integer staffDId) {
    return trainingEntitlementRepo.findByStaff_Id(staffDId);
    }

    public TrainingEntitlement getEntitlementByStaffAndYear(Integer staffDId,
        Integer year) {
        return trainingEntitlementRepo
            .findByStaff_IdAndYear(staffDId, year)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Training entitlement not found for staff id: "
                            + staffDId + " and year: " + year));
    }

}
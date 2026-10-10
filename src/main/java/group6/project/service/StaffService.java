package group6.project.service;

import group6.project.model.Staff;

import org.springframework.stereotype.Service;

@Service
public class StaffService {
    private final TrainingEntitlementService entitlements;

    // Staff owns the personal dashboard; application and claim rules have their own services.
    public StaffService(TrainingEntitlementService entitlements) {
        this.entitlements = entitlements;
    }

    // All workspaces use one annual calculation.
    public TrainingEntitlementService.AnnualSummary summary(Staff employee, int year) {
        return entitlements.summary(employee, year, null);
    }
}

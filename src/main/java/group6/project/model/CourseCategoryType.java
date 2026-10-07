package group6.project.model;

public enum CourseCategoryType {
    INTERNAL_TRAINING,
    EXTERNAL_COURSE,
    PROFESSIONAL_CERTIFICATION;

    public String getDisplayName() {
        return name().replace('_', ' ');
    }
}

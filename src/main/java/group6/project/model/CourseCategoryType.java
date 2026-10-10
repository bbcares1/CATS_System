// Defines the three course types and their display names.
package group6.project.model;

public enum CourseCategoryType {
    INTERNAL_TRAINING,
    EXTERNAL_COURSE,
    PROFESSIONAL_CERTIFICATION;

    // Keep database values stable and use normal labels on forms and reports.
    public String getDisplayName() {
        return switch (this) {
            case INTERNAL_TRAINING -> "Internal Training";
            case EXTERNAL_COURSE -> "External Course";
            case PROFESSIONAL_CERTIFICATION -> "Professional Certification";
        };
    }
}

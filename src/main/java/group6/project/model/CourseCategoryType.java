package group6.project.model;

public enum CourseCategoryType {
    INTERNAL_TRAINING,
    EXTERNAL_COURSE,
    PROFESSIONAL_CERTIFICATION;

    // Human labels are independent of the enum values already stored in the database.
    public String getDisplayName() {
        return switch(this) { case INTERNAL_TRAINING -> "Internal Training"; case EXTERNAL_COURSE -> "External Course";
            case PROFESSIONAL_CERTIFICATION -> "Professional Certification"; };
    }
}

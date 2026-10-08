# Staff and course application integration

## Requirements interpreted from the group conversation

1. Consolidate current employee lookup into StaffService while preserving employee lookup endpoints.
2. Keep course application lifecycle rules in CourseApplicationService; controllers and ManagerService delegate to it.
3. Connect managers to employee applications: pending applications, approval, rejection and employee course history.

## Design assumption

Each Staff has one optional direct Manager (manager_id). Managers can only review or read history for their direct reports. ApprovalHierarchy currently defines roles and levels only, so this change implements one manager decision, not multi-level approval. Existing employees need a manager assignment in the database before appearing in a manager queue. No automatic assignment is inferred.

CourseApplication.applicant remains the owner. reviewedBy, reviewedAt and decisionReason record the decision. Only APPLIED or UPDATED applications can become APPROVED or REJECTED. Rejection requires a nonblank reason. Employee editing, cancellation and completion retain their existing rules.

## Service linkage

- CourseApplicationController -> StaffService.requireStaff -> CourseApplicationService
- ManagerController -> ManagerService.requireManager -> ManagerService -> CourseApplicationService
- CurrentStaffService is a deprecated compatibility adapter to StaffService.

Both identity services support Principal and the existing currentUser / loggedInUser session conventions. Manager identity is resolved from the signed-in account, never a caller-supplied manager ID.

## Manager API

- GET /api/managers/me/course-applications: pending applications of direct reports.
- POST /api/managers/me/course-applications/{id}/approve: optional reason request parameter.
- POST /api/managers/me/course-applications/{id}/reject: required reason request parameter.
- GET /api/managers/me/staff/{staffId}/course-applications: all-year course history of one direct report.

Errors: 401 for missing manager identity, 403 for another manager's employee, 404 for missing application, 409 for already processed application, 400 for missing rejection reason.

## Scope

Fee-claim approval remains a separate unfinished feature. This change does not merge a remote pull request or publish changes. The manager integration supplies JSON endpoints; a manager dashboard is not added.

## Validation

See BACKEND_CHECK.md for current startup configuration and verification. Seed fixtures now use the real single-table User hierarchy and include manager assignments. Tests use the dev profile with seeding enabled.

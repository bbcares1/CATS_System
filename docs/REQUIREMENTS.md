# CA requirements map

This page shows where we implemented each requirement from the SA63 CA instructions (pages 3–11). In our code, **Staff** is the CA's Employee, and **Manager extends Staff**, so managers also have their own Staff pages. We built every feature along the same path: Thymeleaf page → Controller → Service → Repository → MySQL.

## Mandatory features

| Requirement (CA page) | How we built it | Main code |
|---|---|---|
| Admin, Manager and Employee roles; managers are employees (3–4) | `User → Staff → Manager` and `User → Admin` in Java, stored in one `users` table with a `role` column | `model/User`, `Staff`, `Manager`, `Admin` |
| Separate Employee and Admin login (5) | `/employee/login` and `/admin/login`, checked against saved accounts; roles are checked by URL | `UserController`, `UserService.authenticate()`, `config/AuthInterceptor` |
| Internal Training, External Course, Professional Certification (3, 5) | Three fixed category types; Internal Training is always free | `CourseCategoryType`, `CourseApplicationService.validate()` |
| Apply with title, category, provider, dates, fee and justification (6) | Apply from the catalogue or enter another course; the server sets Applied and the approving manager | `CourseApplicationController`, `CourseApplicationService.createOther()`, `createFromCatalogue()` |
| Required fields, future dates, end not before start (6) | Bean Validation on the form, then the same checks in the service | `CourseApplicationForm`, `TrainingDayCalculator.count()` |
| Working start and end dates; weekends and public holidays not counted (6) | One working-day calculator with the saved holidays | `TrainingDayCalculator`, `ExcludedDaysRepo` |
| Half days for Internal Training only (3, 6) | A one-day AM or PM session counts as 0.5 day | `TrainingDayCalculator.count()` |
| Annual training-day and budget limits (6) | Yearly allowance per employee. Applied and Updated reserve; Approved and Completed use | `TrainingEntitlementService.summary()` |
| No overlap with Applied, Updated or Approved courses (6) | Overlap check skips the record being edited; AM and PM of the same day may both exist | `CourseApplicationService.overlaps()` |
| Update before a decision → Updated; delete → Deleted (5–6) | Only Applied or Updated can change; Delete keeps the row as history | `CourseApplicationService.updateOther()`, `delete()` |
| Cancel an approved course → Cancelled; attend with comments → Completed (5–6) | Approved only; comments are required | `CourseApplicationService.cancel()`, `complete()` |
| Current-year personal history with details (6) | Every status, by course start date, with a year filter | `CourseApplicationController.history()`, `details()` |
| Pending applications grouped by subordinate (7) | Applied and Updated requests assigned to the manager, one card per employee | `ManagerController.pending()`, `ManagerService.getPendingApplicationGroups()` |
| Details, annual usage and other subordinates' approved courses in the same period (7) | Review page shows the allowance summary and overlapping approvals | `ManagerController.details()`, `ManagerService.overlaps()` |
| Approve or reject with a reason (7) | A reason is required for both; approval re-runs the date, allowance and overlap checks | `DecisionForm`, `CourseApplicationService.decide()` |
| Subordinate course history (7) | Direct reports only, current year, same view as personal history | `ManagerController.history()`, `ManagerService.history()` |

## Optional features we built

| Requirement (CA page) | How we built it | Main code |
|---|---|---|
| Manage accounts, roles and reporting managers (8) | Role changes keep the same `user_id` and history; accounts with history are disabled, not deleted; no reporting cycles | `AccountAdminController`, `AccountAdminService`, `UserRepo.changeRole()` |
| Maintain categories, course catalogue, providers and public holidays (8) | Records still in use are archived instead of deleted; a holiday cannot fall on a date already booked | `CourseCategoryController`, `CourseDetailController`, `CourseBatchController`, `CourseProviderController`, `ExcludedDaysController` |
| Annual training days and budget per employee (8) | One allowance per employee per year; it cannot drop below what is already reserved or used | `AdminController.saveBudget()`, `TrainingEntitlementService.saveLimits()` |
| Fee claims with receipt and certificate (8) | Completed External Course or Certification only; two files up to 5 MB each; one claim per course | `ClaimController`, `CourseFeeApplicationService.submit()` |
| Manager decides claims; training ledger (8) | A reason is required; Admin records the payment separately, and the fee is not deducted twice | `CourseFeeApplicationService.decide()`, `reimburse()` |
| Attendance and budget/claim reports with CSV export (8) | Filter by period, category and employee; managers see direct reports only | `TrainingReportController`, `TrainingReportService.report()`, `csv()` |
| Training calendar with month navigation (8) | Approved courses on working days, for every role | `TrainingCalendarController`, `TrainingCalendarService.month()` |
| Pagination with page size (8) | 5, 10 or 15 rows per page | `PageSupport.page()`, `fragments/list-pagination.html` |
| Email on submission and decision, with the reason and a login link (9) | Sent after the record is saved; the app still runs without SMTP. Admin can also send email | `NotificationService`, `AdminEmailService` |

We did not choose the REST API with a client (p. 8) or Spring Security (p. 9). Instead, login uses the session, `AuthInterceptor` checks roles and `CsrfInterceptor` protects forms.

## Additional considerations regarding the CA requirements

- For the live demo, we let Mark completed skip the check that the course has ended. This way we can approve a real course and mark it completed in the same demo. Application dates are still validated.
- We keep each course, and each report period, within one calendar year, because allowances are set per year.
- The CA does not say who approves a manager without a reporting manager. We let that manager choose another manager as the approver, and nobody can approve their own request.
- We count pending applications against the allowance, so an employee cannot submit more than the allowance covers.
- Reports can be printed from the browser; we did not build a separate print layout.
- We store passwords in plain text to keep the classroom setup simple. This is not suitable for real accounts.

## Data

[schema.sql](../src/main/resources/db/schema.sql) defines ten tables: `users`, `training_entitlement`, `excluded_days`, `course_category`, `course_provider`, `course_detail`, `course_batch`, `course_application`, `course_fee_application` and `training_calendar_policy`, a single row used to lock the calendar while holidays change. See [DATABASE.md](DATABASE.md) for setup and reset.

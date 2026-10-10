# Team handover

This branch starts from `ef5d7a9`, including the merged login (#24) and Admin (#34) work. The CA requirements decide behaviour; Suria's `SA63/demo` is the main reference for MVC, form validation and service transactions.

The implementation is being integrated in small commits on this branch. The sections below describe completed changes; the final PR will include the remaining modules and acceptance results.

## Login — Junie

- Kept the two login entry points, shared `UserService`, `session.user` and the authentication interceptor.
- Moved the interceptor/configuration from `controller/SecurityForAuthentication` to `config`. It now covers the existing legacy page URLs too.
- A Manager can open Staff pages because `Manager extends Staff`. A valid account with the wrong role gets HTTP 403; an anonymous visitor goes to login.
- The saved account determines the employee role, so the form only asks for username and password. A successful login renews the session ID.
- `UserService.currentUser` reloads the account, rather than trusting a deleted account left in the session. The old application pages now read the same session key.
- Both login forms use `fragments/login.html` and the existing `cats.css`. No new UI framework was added. An accidentally repeated copy of `DESIGN.md` was removed.

Read: `fragments/login.html` → `UserController` → `UserService` → `UserRepo`. For page permissions, read `config/WebConfig` → `config/AuthInterceptor`.

Check: `LoginFlowTest` uses the real Spring context, repository and templates to test the Manager's two workspaces, separate Admin login, role boundaries and context-path redirects. `UserServiceTest` checks credentials, deleted sessions and inheritance.

## Email — Owen

- Kept `AdminEmailService`, its form and the manual send page from #34.
- Made the SMTP dependency optional at startup. An unconfigured send reports an error instead of preventing the whole application from starting.
- Automatic submission and decision notifications are part of the agreed scope and will be integrated with those use cases. They must include the reason and login link where applicable, and must not roll back a saved business decision if SMTP fails.

`AdminEmailServiceTest` still checks the exact recipient, subject and body. Tests use a mock sender and do not email team members.

## Development and deployment

Docker, isolated development/test/deployment databases, repeatable demo data and backup/restore are team decisions. They remain in scope even though the lecturer does not require Docker. They support the classroom application without adding a new Java framework.

The old `imran/final-audit` branch remains a reference for verified rules and tests. It is not merged wholesale. Existing branches, database volumes and the current demo remain intact.

## Accounts and identity — Owen, Jialu, Junie and Imran

- Kept the agreed Java hierarchy. One `users` table now stores Staff, Manager and Admin, so a role change can keep the same database ID and history.
- `AccountAdminController` owns account pages. `AccountForm` sends a `managerId`; the service loads that account instead of binding a nested abstract `User` from the browser. Old account links redirect to these pages.
- `AccountAdminService` is the single write boundary for account changes. It checks unique identifiers, reporting cycles, open work, the last Admin and old form versions. History-bearing accounts are disabled instead of deleted.
- The role update is deliberately contained in `UserRepo.changeRole`: JPA cannot turn a managed Staff object into an Admin with `setRole`. Reloading after the update gives the correct subtype.
- Sessions expire after account changes. Login is case-insensitive for usernames; passwords remain simple classroom credentials and never appear in edit forms.
- Annual days and budgets now live in `TrainingEntitlement`, with one record per employee and year. Staff has no duplicate global allowance fields.

Read: `admin-account-form.html` → `AccountAdminController` → `AccountAdminService` → `UserRepo`.

Check: `AccountAdministrationIntegrationTest` covers identity/history through role changes, reporting cycles, session expiry, deletion protection and rendered forms. All 89 current tests pass against isolated H2; MySQL validation is still pending.

## Annual allowance and dates — Hong Fan, Ryan and Martin

- `TrainingEntitlementService.summary` is the one annual calculation. Applied/Updated reserve allowance; Approved/Completed use it. Rejected/Deleted/Cancelled release it. The pages show reserved and used amounts separately.
- Removed the extra Completed-course calculation and duplicate application validation from `StaffService`. `CourseApplicationService` applies the rules for either employee entry point.
- Application fees use `BigDecimal`. Limits cannot be reduced below amounts already reserved or used. A missing year's allocation is zero until Admin allocates it.
- Admin keeps the budget pages, now with a year selector and `AnnualAllowanceForm`. The designation examples are Administrative 5 days and Professional 10 days; Admin can set a different allocation.
- `TrainingDayCalculator` takes plain dates, category, half-day choice and holidays. It has no repository or session. AM/PM is Internal-only and single-day; multiday courses use full working days.
- Corrected an old overlap bug: an empty half-day value must not avoid a clash with AM or PM. Only opposite AM/PM periods on the same date can coexist.

Check: all 96 tests pass on H2, including annual separation, decimal totals, reservation release, limit reduction and date rules. The old multiday-half-day expectation was changed to the agreed single-day rule, with a full-day overlap regression retained.

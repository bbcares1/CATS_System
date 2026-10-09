# CATS

Java 21, Spring MVC, Thymeleaf and MySQL 8.4. See `DESIGN.md` before changing pages.

## Local development

Start Docker Desktop, then run:

```sh
docker compose up -d --wait
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd`. MySQL listens on `127.0.0.1:3307`, database `cats_dev`.
The default `dev` profile uses the local credentials in `compose.yml` and runs schema migrations plus development samples once. Restarting does not recreate the database.

| Role | Username | Password | Entry |
| --- | --- | --- | --- |
| Admin | admin_alice | demo123 | `/admin/login` |
| Manager | mgr_bob / mgr_chris | demo123 | `/employee/login` |
| Staff | staff_alex / staff_sam / staff_pat | demo123 | `/employee/login` |

Employee login detects Staff or Manager from the saved account; no role selection is needed. This classroom build uses simple login passwords. Do not expose it as a public production service without upgrading authentication.

Samples use fictional accounts and 2026 course dates; they are not production accounts. Additional sample dates should be added in a new development migration, not by editing one that has already run.

For local overrides, copy `.env.example` to `.env`. Compose reads that file; an IDE does not, so also set the matching `CATS_DEV_DB_URL`, `CATS_DEV_DB_USER` and `CATS_DEV_DB_PASSWORD` environment values when overriding application credentials or the port.

## Tests

```sh
./mvnw -B -ntp -DskipTests compile
./mvnw -B -ntp "-Dtest=*ServiceTest,*ControllerTest" test
./mvnw -B -ntp test
```

The full suite uses isolated H2 with the real schema migrations. Check MySQL behaviour too:

After pulling changes to model or service signatures, run `./mvnw -B -ntp clean test` once to remove stale compiled classes.

```sh
docker compose -f compose.test.yml up -d --wait
./mvnw -B -ntp "-Dspring.profiles.active=mysql-test" test
docker compose -f compose.test.yml down
```

The test container uses port 3308 and a disposable database. It never connects to the development volume or the old `group6` database.

## Database changes

Flyway migrations live in `src/main/resources/db/migration`. Add a new version for each schema change. JPA validates the result; do not use `create-drop`, edit applied migrations, or run the old manual ALTER scripts.

The Java hierarchy remains `User → Staff → Manager` and `User → Admin`. Accounts now use one `users` table: the role selects the JPA subtype, and applications, reviewers and annual allowances reference the stable `user_id`. Role changes keep that identity and history. `staff_id` is the readable unique Staff ID for all accounts. The old role-level setup rows are retained in `legacy_approvalhierarchy`; the actual reporting hierarchy uses `users.manager_id`.

Migrations only move forward. Use the newest integration branch with its updated database; an older application version should use a separate database. Preserve a database backup before switching storage models.

Existing `group6` data is not migrated by this setup. Preserve it and plan a separate import if needed.

## Production configuration

Run with `SPRING_PROFILES_ACTIVE=prod` and set `CATS_DB_URL`, `CATS_DB_USER`, `CATS_DB_PASSWORD`. Production loads schema and required categories, never development accounts. Production deployment, initial Admin provisioning and backup instructions are part of the deployment follow-up.

## Application rules

- `/staff/apply` opens the searchable course catalogue; `/staff/apply/other` keeps the form for courses not listed. Catalogue category, provider and fee come from Admin. Employees choose an available session or custom dates when allowed, then enter their reason and optional work arrangements.
- Applications keep the original catalogue details and fee. Admin can archive offers/sessions without removing history; booked session dates cannot be changed. Pending requests reserve a scheduled place until rejected or withdrawn.
- Allowances are allocated per employee and calendar year. No allocation means zero available days/budget.
- Applied, Updated, Approved and Completed courses reserve days and fees. Rejected, Deleted and Cancelled records remain in history but release the reservation.
- Start/end dates must be future working days when applying or editing. Weekends and public holidays are excluded from duration.
- Split cross-year courses into separate requests. AM/PM is allowed only for a single-date Internal Training session; multi-day courses use full working days. Opposite AM/PM sessions can share a date.
- Internal Training has no course fee. Money uses decimal SGD values, with up to two decimal places.
- Complete an approved course only after its end date, with experience comments. Admin cannot lower limits below already reserved days/fees.
- Requests keep their assigned reviewer when the reporting manager changes. Staff use their assigned manager; a Manager without one chooses another active Manager. Self-review is blocked. Admin must resolve assigned pending reviews before removing Manager access.
- `/training/calendar` shows approved working-day attendance to every signed-in role, with month/category filters. It contains names and course details, not private reasons or evidence.
- Manager reports cover current direct reports; Admin reports cover all accounts. Period filters include courses overlapping the dates. Attendance-only reports include Approved/Completed courses. CSV includes every filtered row, with formula-safe cells. Course fees/days are full course amounts; annual allowances always cover the whole year.
- Holiday date changes are blocked while active requests or published future sessions depend on them. Correcting the label is safe.
- Fee claims require a completed, personally paid External Course or Certification and a receipt/certificate (PDF, JPEG or PNG, up to 5 MB each). One claim per course. Both Manager decisions need a reason; Admin records actual payment separately with a reference. Claims never spend the training budget twice.

## MVC entry points

| Workspace | Pages |
| --- | --- |
| Public | `/login`, `/employee/login`, `/admin/login` |
| Staff (including Manager) | `/staff/home`, `/staff/apply`, `/staff/personal`, `/staff/applications/{id}`, `/staff/fee` |
| Shared | `/training/calendar` |
| Manager | `/manager/home`, `/manager/approvals`, `/manager/applications/{id}`, `/manager/history`, `/manager/claims`, `/manager/reports` |
| Admin | `/admin/home`, `/admin/accounts`, `/admin/entitlements`, `/admin/courses`, `/admin/categories`, `/admin/batches`, `/admin/excludedDays`, `/admin/payments`, `/admin/reviews`, `/admin/reports` |

Admin also manages all account types at `/admin/accounts`. Blank edit passwords keep the current password; stored passwords never appear in forms. Changes expire old sessions. Reassign reports before removing Manager access and resolve pending employee requests before disabling them or changing to Admin. Delete is only for unused accounts; disable accounts with history instead. At least one active Admin must remain. Employee annual limits are maintained separately.

Old application, claim and holiday GET routes redirect to these pages. Old POST handlers are retired; use the current forms. All protected prefixes reload the account behind session `user`. Claims and attachments are restricted to the owner or assigned Manager. Reviewers can only decide requests assigned to them; team history still uses direct reports. Password fields are excluded from JSON. CSRF protection is part of the following safety/deployment slice.

Admin can repair unassigned pre-upgrade requests at `/admin/reviews`; the saved ID and decision history stay intact. Legacy batch-only claims use the available catalogue fee during upgrade; verify it against the receipt before recording payment. Records with missing applicants or course data require an explicit import review, rather than guessing ownership. Claim queues, personal claims and payment lists use paged metadata; evidence loads only on an authorized single-claim route.

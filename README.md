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

Choose Staff or Manager on the employee login form. This classroom build uses simple login passwords. Do not expose it as a public production service without upgrading authentication.

Samples use fictional accounts and 2026 course dates; they are not production accounts. Additional sample dates should be added in a new development migration, not by editing one that has already run.

For local overrides, copy `.env.example` to `.env`. Compose reads that file; an IDE does not, so also set the matching `CATS_DEV_DB_URL`, `CATS_DEV_DB_USER` and `CATS_DEV_DB_PASSWORD` environment values when overriding application credentials or the port.

## Tests

```sh
./mvnw -B -ntp -DskipTests compile
./mvnw -B -ntp "-Dtest=*ServiceTest,*ControllerTest" test
./mvnw -B -ntp test
```

The full suite uses isolated H2 with the real schema migrations. Check MySQL behaviour too:

```sh
docker compose -f compose.test.yml up -d --wait
./mvnw -B -ntp "-Dspring.profiles.active=mysql-test" test
docker compose -f compose.test.yml down
```

The test container uses port 3308 and a disposable database. It never connects to the development volume or the old `group6` database.

## Database changes

Flyway migrations live in `src/main/resources/db/migration`. Add a new version for each schema change. JPA validates the result; do not use `create-drop`, edit applied migrations, or run the old manual ALTER scripts.

Existing `group6` data is not migrated by this setup. Preserve it and plan a separate import if needed.

## Production configuration

Run with `SPRING_PROFILES_ACTIVE=prod` and set `CATS_DB_URL`, `CATS_DB_USER`, `CATS_DB_PASSWORD`. Production loads schema and required categories, never development accounts. Production deployment, initial Admin provisioning and backup instructions are part of the deployment follow-up.

## Application rules

- Allowances are allocated per employee and calendar year. No allocation means zero available days/budget.
- Applied, Updated, Approved and Completed courses reserve days and fees. Rejected, Deleted and Cancelled records remain in history but release the reservation.
- Start/end dates must be future working days when applying or editing. Weekends and public holidays are excluded from duration.
- Split cross-year courses into separate requests. AM/PM is allowed only for a single-date Internal Training session; multi-day courses use full working days. Opposite AM/PM sessions can share a date.
- Internal Training has no course fee. Money uses decimal SGD values, with up to two decimal places.
- Complete an approved course only after its end date, with experience comments. Admin cannot lower limits below already reserved days/fees.

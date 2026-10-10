# CATS

Course Application Tracking System for the SA63 Java Web Development CA.
Java 21, Spring Boot, MVC, Thymeleaf, Bootstrap and MySQL.

Course applications, Manager decisions, Admin maintenance, fee claims, reports, a shared calendar and email use ordinary server-rendered pages.

## Start development

Install Java 21 and Docker Desktop, then run from this repository:

```sh
docker compose up -d --wait
./mvnw spring-boot:run
```

On Windows, use `./mvnw.cmd` instead of `./mvnw`. Open **http://localhost:8081**.
The repository Maven Wrapper downloads the matching Maven version; a separate Maven installation is not needed.

All local sample passwords are `demo123`:

| Login | Workspace |
|---|---|
| `admin` | Admin login |
| `manager`, `manager2` | Employee login, Manager and own Staff pages |
| `staff`, `staff2`, `staff3` | Employee login, Staff pages |

These are fictitious accounts with `example.test` email addresses. Development does not send email unless SMTP is configured.
`staff` and `staff2` report to `manager`; `staff3` reports to `manager2`.
The sample catalogue includes Internal Training, an External Course and Professional Certification, with upcoming schedules. The sample history includes every application status, a pending claim, an actual reimbursement and a Manager request assigned to another Manager.

## Run tests

```sh
./mvnw -B -ntp -DskipTests compile
./mvnw -B -ntp "-Dtest=*ServiceTest,*ControllerTest" test
./mvnw -B -ntp test
```

The complete suite uses an isolated in-memory H2 database. To run the same suite on MySQL:

```sh
docker compose -f compose.test.yml up -d --wait
./mvnw -B -ntp "-Dspring.profiles.active=mysql-test" test
```

The MySQL test container has disposable storage. To reset **only automated test data**:

```sh
docker compose -f compose.test.yml down
docker compose -f compose.test.yml up -d --wait
```

## Database environments

| Purpose | Database | Port | Data |
|---|---|---|---|
| Development | `cats-dev` | 3307 | Fixed sample accounts, persistent Docker volume |
| Automated tests | `cats-tests` | 3308 | Test fixtures, disposable Docker storage |
| Deployment | `cats-prod` | Internal Docker network | Own persistent volume, no demo accounts |

Only the development and test databases run locally. The production database runs separately on the deployment server.
Containers use Singapore time for course dates and yearly allowances. Flyway is the schema source; Hibernate validates it. Starting the application does not drop tables or reload saved demo records.
Details for deployment and backup are in [docs/DATABASE.md](docs/DATABASE.md).

## Email

Owen's manual email page remains available under Admin. Application and claim submissions notify the assigned Manager; decisions notify the employee with the saved reason and login link. Mail failure keeps the saved business result and shows a warning.
Enable `dev,mail` for normal STARTTLS SMTP or `dev,qqmail` for QQ, and supply `CATS_MAIL_USER`, `CATS_MAIL_PASSWORD` and, for normal SMTP, `CATS_MAIL_HOST`.
`CATS_MAIL_PORT` defaults to 587. `CATS_MAIL_FROM` can override the sender address.
Set `CATS_PUBLIC_URL` to the URL recipients should open. Use local test addresses or a mail capture server when developing.

Never commit SMTP credentials or a populated `.env.prod`. This classroom application intentionally uses simple login passwords and is intended for controlled course access.

## Windows startup note

If Java reports `Unable to establish loopback connection` while Tomcat starts, use a short local socket directory:

```powershell
New-Item -ItemType Directory -Force .local-tmp
./mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=./.local-tmp"
```

This is a local JDK/Windows path workaround. It does not change database or application code. Docker startup does not need it.

## Try the main flows

1. Log in as `staff`: browse courses or choose **Apply for another course**. Open personal history to edit a pending request, cancel an approved request or read a decision.
2. Log in as `manager`: open **Approvals**, check annual usage and overlapping approved absences, then approve or reject with a reason. Use **My staff workspace** for personal applications.
3. Open **Claim approvals** as `manager` to review Sam's demo claim. As `admin`, open **Reimbursements** and record a payment reference after payment.
4. Open **Training calendar** in any workspace. Manager and Admin **Reports** filter by date, employee and category and export CSV, including annual allowances.
5. As `admin`, maintain accounts/reporting managers, annual allowances, categories, providers, courses, batches and holidays. An unused record can be deleted; referenced records retain history.

Pending applications reserve days and fees. Approved and Completed applications use them; Deleted, Cancelled and Rejected requests release them. Actual reimbursements are shown separately and never spend the course budget twice.
Applications stay within one calendar year. A half-day is a single-day Internal Training session; multi-day courses use full working days. Missing annual allowances must be set by Admin before applying.

## Read the code

Follow a form action from `src/main/resources/templates` to `controller`, then the named use case in `service`, then queries in `repo`. `form` contains permitted input fields; `model` contains the stored records. Services calculate dates, allowances, ownership and state changes; templates display their results.
The final PR explains each member's module changes and the relevant classroom examples. No separate handover document is required.

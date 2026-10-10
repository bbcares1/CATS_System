# CATS

Course Application Tracking System for the SA63 Java Web Development CA.
Java 21, Spring Boot, MVC, Thymeleaf, Bootstrap and MySQL.

This final MVC branch is being integrated in checked commits. The final pull request will explain the module changes and acceptance results.

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
| Development | `cats_final_dev` | 3309 | Fixed sample accounts, persistent Docker volume |
| Automated tests | `cats_final_test` | 3310 | Test fixtures, disposable Docker storage |
| Deployment | `cats_final_prod` | Internal Docker network | Own persistent volume, no demo accounts |

The `final` names separate this version from the earlier integration demo on port 3307. Do not point this branch at that old database.
Containers use Singapore time for course dates and yearly allowances. Flyway is the schema source; Hibernate validates it. Starting the application does not drop tables or reload saved demo records.
Details for deployment, backup and existing databases are in [docs/DATABASE.md](docs/DATABASE.md).

## Email

Owen's manual email page remains available under Admin. The selected final scope also includes submission and decision notifications.
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

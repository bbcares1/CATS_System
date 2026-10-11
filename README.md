# CATS

CATS is our Course Application Tracking System for the SA63 Java Web Development CA.
Employees apply for courses and submit fee claims. Managers review applications and claims. Admins maintain accounts and courses and record reimbursements.

We use Java 21, Spring Boot, Thymeleaf, Bootstrap and MySQL.

## Run locally

Install Java 21 and start Docker Desktop. Use the `main` branch.

### Get the code

For a new clone:

```sh
git clone --branch main https://github.com/bbcares1/CATS_System.git
cd CATS_System
```

For an existing clone, save your local changes and stop Spring Boot before updating:

```sh
git fetch origin
git switch main
git pull --ff-only origin main
```

### Start the application

Run from the project folder in PowerShell:

```powershell
docker compose up -d --wait
.\mvnw.cmd spring-boot:run
```

On macOS or Linux, use `./mvnw` instead of `.\mvnw.cmd`.
Open [http://localhost:8081](http://localhost:8081).

Restarting keeps your data. To restore the initial data, stop Spring Boot and follow the [local reset steps](docs/DATABASE.md#development-restart-and-reset).
For the Windows loopback error, see the [startup workaround](docs/DATABASE.md#windows-startup-error).

## Demo accounts

All demo passwords are `demo123`.

| Login | Workspace |
|---|---|
| `admin` | Admin login |
| `manager`, `manager2` | Employee login, Manager and own Staff pages |
| `staff`, `staff2`, `staff3` | Employee login, Staff pages |

## Project structure

```text
CATS_System/
├── src/
│   ├── main/
│   │   ├── java/group6/project/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── form/
│   │   │   ├── model/
│   │   │   ├── repo/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── db/
│   │       ├── static/
│   │       └── templates/
│   └── test/
├── docs/
│   ├── DATABASE.md
│   ├── DESIGN.md
│   ├── demo/
│   └── deploy/
├── scripts/
├── Dockerfile
├── compose.yml
├── compose.test.yml
├── compose.prod.yml
├── pom.xml
└── README.md
```

Controllers handle requests, services apply business rules, repositories query the database, and models represent stored records. Forms hold user input; templates render the pages. `db` contains the schema and demo data; `test` contains automated tests.

## Tests

Run the tests with H2:

```powershell
.\mvnw.cmd test
```

Or run them with a separate MySQL test database:

```powershell
docker compose -f compose.test.yml up -d --wait
.\mvnw.cmd "-Dspring.profiles.active=mysql-test" test
```

## Email

Manual email is under **Admin > Accounts and managers > Email**. Submissions and decisions also send notifications when SMTP is configured.
For SMTP, use `dev,mail` or `dev,qqmail` and set `CATS_MAIL_USER` and `CATS_MAIL_PASSWORD`. The `mail` profile also needs `CATS_MAIL_HOST`. Set `CATS_PUBLIC_URL` to the application's address. Without SMTP, the application still runs. Keep credentials outside Git.

## Guides

- [UI design](docs/DESIGN.md)
- [Database and local reset](docs/DATABASE.md)
- [Completion, claim and reimbursement demo](docs/demo/README.md)
- [EC2 and RDS deployment: JAR or Docker](docs/deploy/README.md)
- [Deployment maintenance and RDS reset](docs/deploy/MAINTENANCE.md)

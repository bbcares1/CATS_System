# Database

## Schema and demo data

[src/main/resources/db/schema.sql](../src/main/resources/db/schema.sql) defines the tables, indexes, constraints and required categories. [demo-data.sql](../src/main/resources/db/demo-data.sql) contains the fictitious October 2026 demo accounts and business examples.

These are the SQL files used by local development and RDS. There is no development migration history to apply.

| Environment | Schema initialization | Demo data | Storage |
|---|---|---|---|
| Local development (`cats-dev`) | MySQL first initialization | Yes | Persistent Docker volume |
| MySQL tests (`cats-tests`) | MySQL first initialization | No | Disposable Docker storage |
| H2 tests | Spring initializes a fresh database | No | Isolated in memory |
| EC2 demo (your RDS database) | Manual Workbench import | Yes | RDS |

Local Compose loads the SQL only when its database storage is empty. RDS uses the manual import in the deployment guide. Spring Boot validates the schema and does not recreate tables or reload demo data on restart.

## Development restart and reset

Ordinary Spring Boot and Docker restarts keep your saved work. `docker compose down` followed by `docker compose up` also keeps it; do not add `-v` when stopping normally.

To restore the local demo, stop Spring Boot and run one command from the project folder:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\reset-demo.ps1
```

```sh
bash scripts/reset-demo.sh
```

Confirm with `RESET`, then start Spring Boot again. The script backs up the current database in the ignored `backups/` folder and restores only `cats-dev`. It does not reset RDS.

## EC2 and RDS

Follow the [deployment guide](deploy/README.md) for JAR or Docker. Both use the existing SQL and the same RDS database.

Import `schema.sql`, then `demo-data.sql`, into an empty application database before the first deployment. Use your own database name; leave MySQL's `sys` database unchanged. The imported demo data provides the original `demo123` accounts.

Use `cats.env.example` for JAR or `docker.env.example` for Docker; both select `prod` and supply the RDS connection. Replacing the JAR, replacing the application container or restarting keeps RDS data. These actions do not run the SQL again.

To restore the initial RDS demo examples, follow [Reset the RDS demo data](deploy/MAINTENANCE.md#reset-the-rds-demo-data). Back up first, stop CATS, recreate only your demo database, import the original SQL and start CATS again.

`compose.prod.yml` remains an optional Docker setup with its own `cats-prod` database and no demo data.

## Backup and restore

In Workbench, use **Server → Data Export**, select your application database, choose **Dump Structure and Data** and **Export to Self-Contained File**, then start the export.

Keep the export outside Git. It contains account passwords and uploaded claim documents.

To restore, create a new empty database and use **Server → Data Import → Import from Self-Contained File**. Select that new database as the target. Use the matching application version when opening the restored data.

## Windows startup error

If Tomcat reports `Unable to establish loopback connection`, run:

```powershell
New-Item -ItemType Directory -Force .local-tmp
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=./.local-tmp"
```

# Database and deployment

## One schema source

`db/migration` contains schema changes and reference data for every environment.
`db/dev` contains fictitious sample data and is enabled only by the development profile.
Do not edit a migration after it has run on a saved database. Add the next numbered migration instead.
Do not reintroduce `data.sql`, `ddl-auto=create-drop` or manual startup patch scripts.

The final MVC schema starts from a new baseline. It is separate from both the old `group6` JOINED schema and the earlier `cats_dev` V1閳ユ彜14 integration schema.
Flyway must reject an old database with a different migration history. Do not use `repair`, `baseline-on-migrate` or disabled validation to bypass that check.

## Start the deployment locally

1. Copy `.env.prod.example` to `.env.prod` and replace its database and first-Admin credentials.
2. Set `CATS_PUBLIC_URL` to the intended URL, including a context path if used.
3. If email is available, set `CATS_PROFILES=prod,mail` (or `prod,qqmail`) and its SMTP values.
4. Run:

```sh
docker compose --env-file .env.prod -f compose.prod.yml up -d --build --wait
```

The application listens on localhost:8081 by default. The database has no host-published port.
For a server, keep localhost binding behind the team's HTTPS reverse proxy. Change the public URL and access configuration for that environment.

On its first start, the application creates only the Admin specified in `.env.prod`. Later restarts do not reset that account.
Create employee accounts and annual allowances through Admin. No known development password is inserted into the deployment database.
Use `docker compose --env-file .env.prod -f compose.prod.yml down` to stop it while keeping its volume. Do not add `-v` to ordinary stop commands.

## Backup and restore

Take a backup before schema changes. The following works from PowerShell or a shell without piping SQL through a text encoding conversion:

```sh
mkdir backups
docker compose --env-file .env.prod -f compose.prod.yml exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u "$MYSQL_USER" --single-transaction --no-tablespaces --hex-blob --result-file=/tmp/cats-backup.sql "$MYSQL_DATABASE"'
docker compose --env-file .env.prod -f compose.prod.yml cp db:/tmp/cats-backup.sql ./backups/cats-backup.sql
```

Move the export to a private backup location outside Git. It includes account credentials and uploaded claim documents.
To prove a backup is usable, restore it to a **new empty database**, then run the matching application version with `ddl-auto=validate`.
Use the database name and credentials of that isolated restore environment; do not test a restore against the running deployment.

```sh
docker cp ./backups/cats-backup.sql RESTORE_CONTAINER:/tmp/cats-restore.sql
docker exec RESTORE_CONTAINER sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" "$MYSQL_DATABASE" < /tmp/cats-restore.sql'
```

`RESTORE_CONTAINER` means an explicitly created isolated MySQL container. Compare account, application, claim and attachment counts and check login after restoring.
The baseline verification restored a real MySQL export into a separate container: all 6 demo accounts, 10 annual allocations and both migration records matched. The application passed schema validation and Staff login against the restored copy. The final release will repeat this check with course and claim attachments after those workflows are integrated.

## Earlier local databases

Keep the old database and volume intact until the team has checked this version. Do not mount an old data volume into these Compose projects.
The new development database is reproducible demo data, not an automatic import of the previous database.
If the team needs old records, first export them and import into an isolated copy using this mapping:

| Old source | Final target |
|---|---|
| `users` plus `staff`, `manager`, `admin` | One `users` row per stable `user_id`, with the correct role discriminator |
| Staff identifier | `users.staff_id`; do not use this editable code as a foreign key |
| Global days and budget | One explicitly chosen year's `training_entitlement` row; never invent prior-year history |
| Reporting manager | `users.manager_id`, checked for active managers and cycles |
| Course applications | Same applicant ID, dates, fee, status and history; decimal fees |
| Review/claim history | Stable user references, original decision reasons and document bytes |

Older pending requests without a reviewer need an explicit assignment before they can be decided.
Validate counts, relationships, duplicate identifiers and money totals on the import copy before considering a cutover. The source database stays available as the rollback copy.

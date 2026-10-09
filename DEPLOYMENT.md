# Run and recover CATS

Java 21 and Docker Compose are enough. Development, automated tests and production use different databases and volumes. Do not point an old application branch at a newer migrated database.

## First production installation

1. Copy `.env.prod.example` to `.env.prod` and choose your database and initial Admin credentials. Keep this file out of Git. Keep the single quotes around passwords so characters such as `$` stay literal.
2. Start the production stack:

```sh
docker compose --env-file .env.prod -f compose.prod.yml up -d --build --wait
```

Open `http://localhost:8081/admin/login`, or the port you chose. If `CATS_CONTEXT_PATH=/cats`, use `/cats/admin/login`; form actions and health probes include that path. The initial Admin comes only from the environment; no development accounts, courses or sample applications are loaded. Once an active Admin exists, restarts ignore bootstrap credentials and do not reset accounts. Create Managers/Staff and allocate annual allowances using the Admin pages.

The database is internal to the Compose network. The web port binds to localhost by default. The application runs as a non-root user; `/health` returns only UP/DOWN and checks the database. Keep this classroom build on a private network; the README describes its simple-password authentication limitation. This setup does not configure a public host, TLS or a cloud provider.

## Updates and restarts

Back up first, then pull the reviewed version and run the same `up -d --build --wait` command. Flyway applies new migrations; JPA validates the schema. Do not edit applied migrations or use `create-drop`.

```sh
docker compose --env-file .env.prod -f compose.prod.yml restart
docker compose --env-file .env.prod -f compose.prod.yml ps
docker compose --env-file .env.prod -f compose.prod.yml logs --tail 80 app
```

`down` stops containers but preserves the database volume. Do not add `-v` to a production shutdown. Code rollback may require a matching database backup because migrations move forward.

## Backup

Create a local `backups` directory (ignored by Git). Write the dump inside the container and copy it as a file, so Windows shell encoding cannot corrupt it.

```sh
docker compose --env-file .env.prod -f compose.prod.yml exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u"$MYSQL_USER" --single-transaction --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE" > /tmp/cats-backup.sql'
docker compose --env-file .env.prod -f compose.prod.yml cp db:/tmp/cats-backup.sql backups/cats-backup.sql
```

Keep dated copies outside the machine too. Dumps contain accounts and claim evidence, so keep them private.

## Verify a restore without touching the running database

Copy `.env.prod` to `.env.restore` and set its web port to `8082`. Use the new project name `cats-recovery`; its fresh volume is separate from production. Start only the database before importing, so the app cannot migrate or write during restore.

```sh
docker compose --env-file .env.restore -p cats-recovery -f compose.prod.yml up -d --wait db
docker compose --env-file .env.restore -p cats-recovery -f compose.prod.yml cp backups/cats-backup.sql db:/tmp/cats-restore.sql
docker compose --env-file .env.restore -p cats-recovery -f compose.prod.yml exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" < /tmp/cats-restore.sql'
docker compose --env-file .env.restore -p cats-recovery -f compose.prod.yml up -d --wait app
```

Check `/health`, log in with a restored account and inspect saved courses/claims. A verified recovery can be used for a deliberate cutover later; this procedure does not replace the running production volume.

## Automated checks

CI runs compile and fast Service/Controller tests first. Full H2 context/MVC/integration tests and the disposable MySQL suite start only after that job passes. All jobs are read-only, bounded and fail normally. PRs targeting `main` or an `imran/**` integration base, pushes to `main` and manual runs are supported.

For the same local checks, see README. Production startup and restore validation use a separate temporary Compose project, never the team's development database.

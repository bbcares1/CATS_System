# Production deployment with Docker Compose

We use this for a deployment without demo data. The [EC2 and RDS guide](README.md) sets up our shared classroom demo instead.

`compose.prod.yml` runs CATS and its own MySQL database (`cats-prod`) on one server. On first start, MySQL creates the tables from `schema.sql` and loads no demo accounts or sample records. CATS then creates one Admin from `.env.prod`; later restarts keep that account.

We store login passwords in plain text to keep this classroom project simple. Use unique passwords, serve CATS over HTTPS and do not reuse real passwords.

## First start

1. Install Docker with Compose on the server and copy the project folder there.
2. Create the settings file. It is ignored by Git:

   ```sh
   cp .env.prod.example .env.prod
   ```

3. In `.env.prod`, set the two database passwords, the first Admin (username, password, name, Staff ID and email) and `CATS_PUBLIC_URL`. Put any value containing `$` in single quotes.
4. Email is optional. To send it, set `CATS_PROFILES=prod,mail` (or `prod,qqmail`) and the SMTP values.
5. Start CATS:

   ```sh
   docker compose --env-file .env.prod -f compose.prod.yml up -d --build --wait
   ```

6. Check `curl -i http://127.0.0.1:8081/health` returns **UP**. Log in at `/admin/login` with the Admin from `.env.prod`, then add accounts, reporting managers and annual allowances.

CATS listens on `127.0.0.1:8081` by default, and the database port is not published. Put an HTTPS reverse proxy in front of it, or change `CATS_BIND_ADDRESS` only if you accept direct access.

## Stop and update

- Stop: `docker compose --env-file .env.prod -f compose.prod.yml down`. The database volume is kept. Do not add `-v`.
- Update: get the new code and run the start command again. Saved data stays; Spring Boot only validates the schema. If a release changes the schema, follow its database notes before starting it.

## Back up

```sh
mkdir -p backups
docker compose --env-file .env.prod -f compose.prod.yml exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u "$MYSQL_USER" --single-transaction --no-tablespaces --hex-blob --result-file=/tmp/cats-backup.sql "$MYSQL_DATABASE"'
docker compose --env-file .env.prod -f compose.prod.yml cp db:/tmp/cats-backup.sql ./backups/cats-backup.sql
```

Move the file to private storage. It contains account passwords and uploaded claim documents. To restore, import it into a new empty MySQL database and run the same CATS version against that database.

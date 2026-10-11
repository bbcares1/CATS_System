# Deploy CATS to EC2 and RDS

Use the `imran/final-mvc` branch.

## 1. Build the JAR on your computer

Open PowerShell in your `CATS_System` project folder and run:

```powershell
.\mvnw.cmd -DskipTests package
```

The JAR is `target/project-0.0.1-SNAPSHOT.jar`.

## 2. Load the database in Workbench

Open your RDS connection in MySQL Workbench. Use an empty application database. If you have no application database yet, run ``CREATE DATABASE `cats-demo`;`` first. Leave the MySQL `sys` database unchanged.

With **File → Open SQL Script**, open these repository files in order:

1. [src/main/resources/db/schema.sql](../../src/main/resources/db/schema.sql)
2. [src/main/resources/db/demo-data.sql](../../src/main/resources/db/demo-data.sql)

At the top of each opened script, add these two lines, replacing `cats-demo` with your database name:

```sql
USE `cats-demo`;
SET SESSION time_zone = '+08:00';
```

Then click the first lightning button to run the whole script. These lines are for the import; you do not need to save changes to the repository SQL files. Use the database name under **SCHEMAS**; the RDS instance name may differ.

Run these files only for the first setup. Restarting or updating the application keeps your data.

## 3. Prepare EC2

In PowerShell, replace `YOUR_KEY.pem` with your key file path and `YOUR_EC2_IP` with your EC2 public IPv4 address:

```powershell
ssh -i "YOUR_KEY.pem" ec2-user@YOUR_EC2_IP
```

In the EC2 terminal:

```bash
sudo dnf install -y java-21-amazon-corretto-headless nano
mkdir -p ~/cats
```

## 4. Upload the files

Keep the EC2 terminal open. Open another PowerShell window in your `CATS_System` folder:

```powershell
scp -i "YOUR_KEY.pem" target/project-0.0.1-SNAPSHOT.jar ec2-user@YOUR_EC2_IP:~/cats/app.jar
scp -i "YOUR_KEY.pem" docs/deploy/cats.env.example docs/deploy/cats.service ec2-user@YOUR_EC2_IP:~/cats/
```

## 5. Start the application

In the EC2 terminal:

```bash
cd ~/cats
cp cats.env.example cats.env
nano cats.env
```

Replace the placeholders with your RDS endpoint, RDS username, RDS password and EC2 public IP. The database name in `CATS_DB_URL` must match the database used in step 2; the template uses `cats-demo`.

Save with **Ctrl+O**, **Enter**, then exit with **Ctrl+X**.

```bash
chmod 600 cats.env
sudo cp cats.service /etc/systemd/system/cats.service
sudo systemctl daemon-reload
sudo systemctl enable --now cats
```

The application stays running after you close the SSH terminal.

## 6. Open CATS

In AWS, open **EC2 → Security Groups → your EC2 security group → Edit inbound rules**. Add **Custom TCP**, port **8081**, source **My IP**. Each teammate can add their own IP to use the link.

Open `http://YOUR_EC2_IP:8081` in your browser.

All demo passwords are `demo123`.

| Login | Page |
|---|---|
| `admin` | `/admin/login` |
| `manager`, `manager2` | `/employee/login`, Manager and own Staff pages |
| `staff`, `staff2`, `staff3` | `/employee/login`, Staff pages |

## Update later

Build the new JAR with step 1, upload it with the first command in step 4, then run on EC2:

```bash
sudo systemctl restart cats
```

Do not run the SQL files again. Your existing data stays in RDS.

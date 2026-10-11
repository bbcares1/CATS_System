# Deploy CATS to EC2 and RDS

Use the `imran/final-mvc` branch. Choose JAR or Docker; both connect to RDS.

Complete [First setup](#first-setup), then follow one method:

| Method | Runs on EC2 |
|---|---|
| [Deploy with JAR (No Docker)](#deploy-with-jar-no-docker) | Java 21 and systemd |
| [Deploy with Docker](#deploy-with-docker) | Docker |

## First setup

Complete this setup before following either deployment method.

### Load the RDS database

Open your RDS connection in Workbench. Use an empty application database. If needed, run ``CREATE DATABASE `cats-demo`;``. Leave `sys` unchanged.

With **File → Open SQL Script**, open these files in order:

1. [schema.sql](../../src/main/resources/db/schema.sql)
2. [demo-data.sql](../../src/main/resources/db/demo-data.sql)

At the top of each opened script, temporarily add:

```sql
USE `cats-demo`;
SET SESSION time_zone = '+08:00';
```

Replace `cats-demo` with your database name under **SCHEMAS**, then click the first lightning button to run the whole script. The RDS instance name may differ from the database name. Do not save these extra lines to the repository SQL files.

Import only for the first setup. Restarts and updates keep the RDS data.

### Connect to EC2

In your computer's PowerShell, replace `YOUR_KEY.pem` with your key path and `YOUR_EC2_IP` with the current EC2 public IPv4 address:

```powershell
ssh -i "YOUR_KEY.pem" ec2-user@YOUR_EC2_IP
```

In the EC2 terminal:

```bash
mkdir -p ~/cats
```

Keep this terminal open. Upload commands below run in another PowerShell window in your local `CATS_System` folder.

### Allow access to CATS

In AWS, open **EC2 → your instance → Security → its security group → Edit inbound rules**. For a shared demo link, add **Custom TCP**, port **8081**, source **Anywhere-IPv4** (`0.0.0.0/0`). **My IP** allows only your current public IP.

## Deploy with JAR (No Docker)

### 1. Build on your computer

In PowerShell in `CATS_System`:

```powershell
.\mvnw.cmd -DskipTests package
```

Continue after `BUILD SUCCESS`.

### 2. Prepare EC2

In the EC2 terminal:

```bash
sudo dnf install -y java-21-amazon-corretto-headless nano
mkdir -p ~/cats
```

### 3. Upload from your computer

```powershell
scp -i "YOUR_KEY.pem" target/project-0.0.1-SNAPSHOT.jar ec2-user@YOUR_EC2_IP:~/cats/app.jar
scp -i "YOUR_KEY.pem" docs/deploy/cats.env.example docs/deploy/cats.service ec2-user@YOUR_EC2_IP:~/cats/
```

Wait for SCP to finish.

### 4. Configure and start on EC2

```bash
cd ~/cats
cp cats.env.example cats.env
nano cats.env
```

Fill in your RDS endpoint, database name, RDS username, RDS password and EC2 public IP. Save with **Ctrl+O**, **Enter**, then **Ctrl+X**.

```bash
chmod 600 cats.env
sudo cp cats.service /etc/systemd/system/cats.service
sudo systemctl daemon-reload
sudo systemctl enable --now cats
```

After Spring Boot starts:

```bash
sudo systemctl status cats --no-pager -l
curl -i --max-time 10 http://127.0.0.1:8081/health
```

Expected: **active (running)** and **HTTP 200** with **UP**.

## Deploy with Docker

### 1. Build and save on your computer

Start Docker Desktop. In PowerShell in `CATS_System`:

```powershell
docker build --platform linux/amd64 -t cats-system:demo .
docker save -o cats-image.tar cats-system:demo
```

The existing Dockerfile builds the JAR and includes Java. No separate Maven command is needed. The image matches the team's x86_64 EC2 instances.

### 2. Prepare EC2

Connect using the SSH command above. In the EC2 terminal:

```bash
sudo dnf install -y docker nano
sudo systemctl enable --now docker
mkdir -p ~/cats
```

### 3. Upload and load

In your computer's PowerShell:

```powershell
scp -i "YOUR_KEY.pem" cats-image.tar docs/deploy/docker.env.example ec2-user@YOUR_EC2_IP:~/cats/
```

Wait for SCP to finish. In the EC2 terminal:

```bash
sudo docker load -i ~/cats/cats-image.tar
cd ~/cats
```

Expected: `Loaded image: cats-system:demo`.

### 4. Configure Docker

```bash
cd ~/cats
cp docker.env.example docker.env
nano docker.env
```

Fill in your RDS endpoint, database name, username, password and EC2 public IP. Do not put enclosing quotes around Docker values. Keep `CATS_HTTP_PORT=8080`; `CATS_PUBLIC_URL` uses port **8081**.

Save with **Ctrl+O**, **Enter**, then **Ctrl+X**.

```bash
chmod 600 docker.env
```

### 5. Start the Docker container

In the EC2 terminal:

```bash
sudo docker run -d --name cats --restart unless-stopped \
  --env-file /home/ec2-user/cats/docker.env \
  -p 8081:8080 cats-system:demo
```

After Spring Boot starts:

```bash
sudo docker ps --filter name=cats
curl -i --max-time 10 http://127.0.0.1:8081/health
```

Expected: the container is **Up**, and `/health` returns **HTTP 200** with **UP**. Docker runs the application; the database stays in RDS.

## Open CATS

Both methods use `http://YOUR_EC2_IP:8081/employee/login`. Admin uses `/admin/login`. All demo passwords are `demo123`.

| Login | Page |
|---|---|
| `admin` | `/admin/login` |
| `manager`, `manager2` | `/employee/login`, Manager and own Staff pages |
| `staff`, `staff2`, `staff3` | `/employee/login`, Staff pages |

## Maintenance

See the [maintenance guide](MAINTENANCE.md) for service commands, updates, RDS demo reset and AWS Labs IP changes.

## If the page does not open

| Check | JAR | Docker |
|---|---|---|
| Recent logs on EC2 | `sudo journalctl -u cats -n 60 --no-pager` | `sudo docker logs --tail 60 cats` |
| Running state | `sudo systemctl status cats --no-pager -l` | `sudo docker ps -a --filter name=cats` |

If local `/health` is `UP` but the browser cannot connect, check the current EC2 public IP and its TCP 8081 source rule. If `/health` returns `DOWN`, check the RDS settings and application logs.

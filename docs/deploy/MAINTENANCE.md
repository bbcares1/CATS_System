# Maintain the EC2 demo

Connect with the SSH command in the [deployment guide](README.md#connect-to-ec2). Use the column for your current deployment method.

## Stop, start or restart CATS

Run on EC2:

| Action | JAR | Docker |
|---|---|---|
| Stop | `sudo systemctl stop cats` | `sudo docker stop cats` |
| Start | `sudo systemctl start cats` | `sudo docker start cats` |
| Restart | `sudo systemctl restart cats` | `sudo docker restart cats` |
| Status | `sudo systemctl status cats --no-pager -l` | `sudo docker ps -a --filter name=cats` |
| Recent logs | `sudo journalctl -u cats -n 60 --no-pager` | `sudo docker logs --tail 60 cats` |

These operations keep RDS data. Stopping CATS does not stop EC2 or RDS. The enabled JAR service starts when EC2 boots; Docker resumes a container that was running before shutdown. After a manual Docker stop, use `docker start` to resume it.

After starting or restarting, wait for Spring Boot to start:

```bash
curl -i --max-time 10 http://127.0.0.1:8081/health
```

Expected: HTTP 200 and `UP`.

## Update a JAR deployment

1. On your computer, in `CATS_System` on `imran/final-mvc`:

   ```powershell
   git pull --ff-only origin imran/final-mvc
   .\mvnw.cmd -DskipTests package
   ```

2. After `BUILD SUCCESS`, run on EC2:

   ```bash
   sudo systemctl stop cats
   cp ~/cats/app.jar ~/cats/app.jar.previous
   ```

3. Upload from your computer, using the current IP and your key:

   ```powershell
   scp -i "YOUR_KEY.pem" target/project-0.0.1-SNAPSHOT.jar ec2-user@YOUR_EC2_IP:~/cats/app.jar
   ```

4. Wait for SCP to finish, then run on EC2:

   ```bash
   sudo systemctl start cats
   ```

Keep the existing `cats.env` and check `/health`.

If the update fails and the database schema has not changed:

```bash
sudo systemctl stop cats
cp ~/cats/app.jar.previous ~/cats/app.jar
sudo systemctl start cats
```

## Update a Docker deployment

1. Start Docker Desktop. On your computer, in `CATS_System` on `imran/final-mvc`:

   ```powershell
   git pull --ff-only origin imran/final-mvc
   docker build --platform linux/amd64 -t cats-system:demo .
   docker save -o cats-image.tar cats-system:demo
   scp -i "YOUR_KEY.pem" cats-image.tar ec2-user@YOUR_EC2_IP:~/cats/
   ```

2. Wait for SCP to finish. On EC2, keep a tag for the old image and load the new one:

   ```bash
   sudo docker tag cats-system:demo cats-system:previous
   sudo docker load -i ~/cats/cats-image.tar
   ```

3. Replace the application container, keeping `docker.env`:

   ```bash
   sudo docker stop cats
   sudo docker rm cats
   sudo docker run -d --name cats --restart unless-stopped \
     --env-file /home/ec2-user/cats/docker.env \
     -p 8081:8080 cats-system:demo
   ```

Check `/health`. `docker restart` alone does not replace the image.

If the update fails and the database schema has not changed, run `sudo docker tag cats-system:previous cats-system:demo`, then repeat step 3 to use the previous image.

Neither update method reloads SQL. If a future update changes the database schema, follow that update's database instructions.

## Reset the RDS demo data

This removes all changes in your demo database and restores the initial examples and `demo123` accounts. Use it only for your demo database.

1. In Workbench, back up that database with **Server → Data Export → Dump Structure and Data → Export to Self-Contained File → Start Export**. Keep the export outside Git.
2. Stop CATS using the JAR or Docker command in the first table.
3. In your RDS Workbench connection, run the following. Replace `cats-demo` with your demo database name in both lines:

   ```sql
   DROP DATABASE `cats-demo`;
   CREATE DATABASE `cats-demo`;
   ```

4. Repeat [Load the RDS database](README.md#load-the-rds-database): import the existing `schema.sql`, then `demo-data.sql`, selecting the same demo database and `+08:00` session time zone. Leave `sys` unchanged.
5. Start CATS using your deployment method, check `/health` and log in again.

Removing or restarting the application container keeps RDS data. The local `reset-demo` scripts reset Docker development data only; they do not reset RDS.

## After restarting AWS Labs

Normal **End Lab → Start Lab** keeps the EC2 disk files, installed Docker, loaded images, containers and RDS data. EC2's automatically assigned public IPv4 address changes. [AWS Learner Lab guide](https://d1.awsstatic.com/AWS%20Academy%20Learner%20Lab%20Educator%20Guide.pdf), [EC2 stop/start](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/how-ec2-instance-stop-start-works.html).

1. Find the new EC2 **Public IPv4 address**. Use it for SSH, SCP and the browser. Update Workbench's **SSH Hostname** if its connection goes through EC2.
2. On EC2, open the file for your method:

   | Method | Command | Line to update |
   |---|---|---|
   | JAR | `nano ~/cats/cats.env` | `CATS_PUBLIC_URL="http://YOUR_NEW_EC2_IP:8081"` |
   | Docker | `nano ~/cats/docker.env` | `CATS_PUBLIC_URL=http://YOUR_NEW_EC2_IP:8081` |

   Save with **Ctrl+O**, **Enter**, then **Ctrl+X**. This setting supplies the login link in notification emails.

3. For JAR, apply the change:

   ```bash
   sudo systemctl restart cats
   ```

   For Docker, recreate the container so it reads the changed environment file:

   ```bash
   sudo docker stop cats
   sudo docker rm cats
   sudo docker run -d --name cats --restart unless-stopped \
     --env-file /home/ec2-user/cats/docker.env \
     -p 8081:8080 cats-system:demo
   ```

   `docker restart` does not read changes from `docker.env`.

4. Open `http://YOUR_NEW_EC2_IP:8081/employee/login` and share the new link. RDS connection settings and the existing 8081 **Anywhere-IPv4** rule stay the same.

An EC2 **Reboot** normally keeps the public IP. AWS Labs **Reset** deletes lab resources; it is not the CATS demo-data reset. [AWS Learner Lab guide](https://d1.awsstatic.com/AWS%20Academy%20Learner%20Lab%20Educator%20Guide.pdf), [EC2 instance states](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-lifecycle.html).

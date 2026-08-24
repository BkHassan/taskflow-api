# Manual tasks (AI cannot finish these)

These steps are required for a complete junior portfolio. The application code, tests, Docker files, and GitHub Actions **CI** are in the repo. **AWS, GitHub secrets, JDK on your machine, and a real `docker compose` run** are not.

Work through them in order.

---

## 1. Install JDK 17 on Windows

Java is not on `PATH` on this machine. Docker can build the app, but interviews and `./mvnw` locally need a JDK.

1. Install Temurin 17: https://adoptium.net/temurin/releases/?version=17
2. Or in PowerShell (Admin):
   ```powershell
   winget install EclipseAdoptium.Temurin.17.JDK
   ```
3. Close and reopen the terminal, then check:
   ```powershell
   java -version
   ```
   You should see `17`.
4. If `java` is still missing, set `JAVA_HOME` to the Temurin folder (example: `C:\Program Files\Eclipse Adoptium\jdk-17...`) and add `%JAVA_HOME%\bin` to `PATH`.

---

## 2. Run the API locally with Docker

Docker Desktop is already installed. This is the command from the job offer.

1. Open Docker Desktop and wait until it is running.
2. In PowerShell:
   ```powershell
   cd D:\taskflow-api
   copy .env.example .env
   docker compose up --build
   ```
3. Wait until the `api` container logs `Started Application`.
4. Open:
   - API: http://localhost:8080/actuator/health
   - Swagger: http://localhost:8080/swagger-ui.html
5. In Swagger:
   1. `POST /api/auth/login` with demo user `demo` / `Demo123!`
   2. Copy the `token`
   3. Click **Authorize**, paste `Bearer <token>` (or just the token if the UI adds Bearer)
   4. Call `GET /api/tasks` and `POST /api/tasks`
6. Login as admin `admin` / `Admin123!` and call `GET /api/admin/users`.
7. Stop with `Ctrl+C`, then `docker compose down`.

If port 8080 or 5432 is busy, stop the other program or change the ports in `docker-compose.yml`.

---

## 3. Run tests on your machine (after JDK 17)

```powershell
cd D:\taskflow-api
.\mvnw.cmd -B test
```

All tests must be green before you talk about this project in an interview.

---

## 4. Push the code to GitHub

Only after you have reviewed the code (you must be able to explain JWT, BCrypt, and the USER vs ADMIN rule).

```powershell
cd D:\taskflow-api
git status
git add .
git commit -m "Implement JWT auth, task CRUD, Docker and CI"
git push origin main
```

Then open the **Actions** tab: the **CI** workflow must show a green check. If it is red, open the log, fix the failure, push again. Do not put a red CI badge on a CV.

---

## 5. AWS EC2 (the cloud part of the job offer)

AI cannot create your AWS account, card, SSH key, or security group.

### 5.1 Account and region

1. Create an AWS account: https://aws.amazon.com/
2. Sign in to the console.
3. Pick a region close to you (example: `eu-west-3` Paris) and stay in that region for every next step.

### 5.2 Key pair

1. EC2 → **Key Pairs** → **Create key pair**
2. Name: `taskflow-ec2`
3. Type: RSA, format: `.pem`
4. Download the file and store it privately (never commit it).
5. On Windows, restrict permissions if `ssh` complains:
   ```powershell
   icacls $env:USERPROFILE\Downloads\taskflow-ec2.pem /inheritance:r
   icacls $env:USERPROFILE\Downloads\taskflow-ec2.pem /grant:r "$($env:USERNAME):(R)"
   ```

### 5.3 Security group

1. EC2 → **Security Groups** → **Create**
2. Name: `taskflow-sg`
3. Inbound rules:
   - SSH `22` from **My IP** only
   - Custom TCP `8080` from **My IP** (or `0.0.0.0/0` if you want a public demo; understand the risk)
4. Outbound: leave default (all).

### 5.4 Launch the instance

1. EC2 → **Launch instance**
2. Name: `taskflow-api`
3. AMI: **Ubuntu 22.04 LTS**
4. Type: **t2.micro** or **t3.micro** (free-tier eligible if your account still has it)
5. Key pair: `taskflow-ec2`
6. Security group: `taskflow-sg`
7. Storage: 8 GB is enough
8. Launch, then copy the **Public IPv4** (this is `EC2_HOST`)

### 5.5 Install Docker on the instance

From PowerShell:

```powershell
ssh -i $env:USERPROFILE\Downloads\taskflow-ec2.pem ubuntu@YOUR_EC2_PUBLIC_IP
```

On the server:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker ubuntu
exit
```

SSH again (group membership applies on a new session), then:

```bash
docker --version
docker compose version
```

### 5.6 Deploy the app on EC2

Still on the server:

```bash
sudo mkdir -p /opt/taskflow-api
sudo chown ubuntu:ubuntu /opt/taskflow-api
git clone https://github.com/BkHassan/taskflow-api.git /opt/taskflow-api
cd /opt/taskflow-api
nano .env
```

Put real values in `.env`:

```
JWT_SECRET=generate-a-long-random-string-at-least-32-bytes
POSTGRES_PASSWORD=a-strong-database-password
SEED_DEMO_USERS=true
```

Generate a secret on the server:

```bash
openssl rand -base64 48
```

Then:

```bash
docker compose up --build -d
docker compose ps
curl http://localhost:8080/actuator/health
```

From your PC, open `http://YOUR_EC2_PUBLIC_IP:8080/swagger-ui.html`.

### 5.7 Elastic IP (optional but recommended)

EC2 public IPs change if you stop the instance. Allocate an **Elastic IP** and associate it with the instance so the demo URL stays stable.

---

## 6. GitHub secrets for the Deploy workflow

The file `.github/workflows/deploy.yml` is **manual** (`workflow_dispatch`). It will fail until these secrets exist.

1. GitHub repo → **Settings** → **Secrets and variables** → **Actions**
2. Create:
   - `EC2_HOST` = public IP or Elastic IP
   - `EC2_USER` = `ubuntu`
   - `EC2_SSH_KEY` = **full content** of the `.pem` file, including `BEGIN` / `END` lines
3. Actions → **Deploy to EC2** → **Run workflow**
4. Confirm the job is green and Swagger still loads on EC2.

Do not switch this workflow to `on: push` until this run is green.

---

## 7. What you must be able to explain in an interview

Read the "Décisions d'architecture" section in `README.md` and practice out loud:

1. Why JWT is stateless and why CSRF is disabled
2. Why passwords are hashed with BCrypt, never stored in plain text
3. Why `USER` only sees their tasks and `ADMIN` sees all
4. Why the API returns DTOs, not JPA entities
5. Why `open-in-view` is false
6. Difference between CI (GitHub Actions `verify`) and CD (SSH + docker compose on EC2)

If you cannot explain a file, do not list the technology on your CV yet.

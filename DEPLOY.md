# Slate Backend — Hetzner VServer Deployment (Ubuntu)

## 1. Server vorbereiten

```bash
# System updaten
sudo apt update && sudo apt upgrade -y

# Benötigte Tools installieren
sudo apt install -y git curl ufw
```

---

## 2. Docker installieren

> Java muss **nicht** direkt auf dem Server installiert werden — die App läuft im Docker-Container. PostgreSQL läuft hingegen nativ auf dem Server (siehe Schritt 3).

```bash
# Docker GPG Key + Repository hinzufügen
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Docker ohne sudo nutzbar machen (danach neu einloggen!)
sudo usermod -aG docker $USER
newgrp docker

# Testen
docker --version
docker compose version
```

---

## 3. PostgreSQL 16 installieren

```bash
# PostgreSQL Repository hinzufügen
sudo apt install -y gnupg
curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo gpg --dearmor -o /etc/apt/keyrings/postgresql.gpg

echo "deb [signed-by=/etc/apt/keyrings/postgresql.gpg] https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" | \
  sudo tee /etc/apt/sources.list.d/pgdg.list > /dev/null

sudo apt update
sudo apt install -y postgresql-16

# Testen
sudo systemctl status postgresql
```

### Datenbank und User anlegen

```bash
sudo -u postgres psql
```

Im psql-Prompt:

```sql
CREATE USER slate WITH PASSWORD 'sicheres_passwort_hier';
CREATE DATABASE slate OWNER slate;
\q
```

### PostgreSQL für Docker-Zugriff konfigurieren

Der Docker-Container verbindet sich über `host.docker.internal` mit der nativen PostgreSQL-Instanz. Dafür muss PostgreSQL Verbindungen von Docker-Netzwerkadressen akzeptieren:

```bash
# PostgreSQL-Konfigurationsdateien finden
sudo -u postgres psql -c "SHOW config_file;"
sudo -u postgres psql -c "SHOW hba_file;"

# postgresql.conf: listen_addresses anpassen
sudo nano /etc/postgresql/16/main/postgresql.conf
```

Zeile finden und ändern:
```
listen_addresses = 'localhost'   →   listen_addresses = '*'
```

```bash
# pg_hba.conf: Docker-Subnetz erlauben
sudo nano /etc/postgresql/16/main/pg_hba.conf
```

Am Ende der Datei hinzufügen:
```
# Allow Docker containers
host    slate    slate    172.17.0.0/16    scram-sha-256
```

```bash
# PostgreSQL neu starten
sudo systemctl restart postgresql
```

---

## 4. Node.js installieren (für Next.js Frontend)

```bash
# Node.js 20 LTS via NodeSource
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# Testen
node --version
npm --version
```

---

## 5. Firewall konfigurieren

```bash
sudo ufw allow OpenSSH
sudo ufw allow 8080/tcp   # temporär während Setup (wird später durch Nginx ersetzt)
sudo ufw enable
sudo ufw status
```

---

## 6. Projekt auf den Server bringen

```bash
# Projektverzeichnis anlegen
mkdir -p ~/apps/slate && cd ~/apps/slate

# Per Git klonen
git clone https://github.com/jodegen/slate-backend.git .
```

---

## 7. Umgebungsvariablen konfigurieren

```bash
cd ~/apps/slate

# .env aus dem Beispiel erstellen
cp .env.example .env

# JWT Secret generieren und kopieren
openssl rand -base64 64

# .env befüllen
nano .env
```

Vollständiger Inhalt der `.env`:

```env
# Datenbank — host.docker.internal erreicht PostgreSQL auf dem Host
DB_URL=jdbc:postgresql://host.docker.internal:5432/slate
DB_USER=slate
DB_PASSWORD=sicheres_passwort_hier

# JWT — den generierten Base64-String von openssl einfügen
JWT_SECRET=dein-generierter-base64-string
JWT_EXPIRY_MS=900000
JWT_REFRESH_EXPIRY_MS=2592000000

# CORS — URL deines Next.js Frontends
CORS_ALLOWED_ORIGINS=https://slate.jodegen.de
```

> **Hinweis:** `nano` speichern mit `Ctrl+O`, beenden mit `Ctrl+X`

> **Wichtig:** `host.docker.internal` ist der spezielle Hostname, über den Docker-Container den Host-Server erreichen. Das funktioniert durch `extra_hosts` in `docker-compose.yml`.

---

## 8. Starten

```bash
# Image bauen und Stack starten
docker compose up --build -d

# Logs prüfen
docker compose logs -f app

# Status prüfen
docker compose ps
```

Die API ist jetzt unter `http://DEINE-SERVER-IP:8080` erreichbar.

---

## 9. Testen

```bash
# Health-Check — sollte 401 zurückgeben (API läuft)
curl -s http://localhost:8080/api/users/me

# Registrierung testen
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!","name":"Test User"}' | python3 -m json.tool
```

---

## 10. Nützliche Befehle

```bash
# Stack stoppen
docker compose down

# Stack stoppen + Datenbank löschen (VORSICHT!)
docker compose down -v

# Neu bauen und starten (nach Code-Änderungen)
docker compose up --build -d

# App-Logs live verfolgen
docker compose logs -f app

# In die Datenbank schauen
docker compose exec db psql -U slate -d slate

# Container-Status
docker compose ps
```

---

## 11. Updates deployen

```bash
cd ~/apps/slate

# Neuesten Code holen
git pull

# Neu bauen und starten (Downtime: ~30 Sek.)
docker compose up --build -d
```

---

## 12. Nginx als Reverse Proxy + HTTPS

Da das Frontend (Next.js) ebenfalls auf dem Server laufen wird, übernimmt Nginx das Routing für beide Domains:
- `api.slate.jodegen.de` → Backend (Port 8080)
- `slate.jodegen.de` → Next.js Frontend (Port 3000)

### Nginx installieren

```bash
sudo apt install -y nginx certbot python3-certbot-nginx

# Port 80 + 443 in der Firewall freigeben
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw delete allow 8080/tcp   # Port 8080 nicht mehr direkt erreichbar
```

### Nginx-Config für das Backend

```bash
sudo nano /etc/nginx/sites-available/slate-api
```

```nginx
server {
    server_name api.slate.jodegen.de;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Nginx-Config für das Next.js Frontend

```bash
sudo nano /etc/nginx/sites-available/slate-frontend
```

```nginx
server {
    server_name slate.jodegen.de;

    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Für Next.js Hot Reload (Entwicklung) — in Produktion nicht nötig
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_cache_bypass $http_upgrade;
    }
}
```

### Aktivieren & testen

```bash
sudo ln -s /etc/nginx/sites-available/slate-api /etc/nginx/sites-enabled/
sudo ln -s /etc/nginx/sites-available/slate-frontend /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

### SSL-Zertifikate ausstellen (Let's Encrypt)

> **Voraussetzung:** Beide DNS-Einträge müssen bereits auf die Server-IP zeigen, bevor du Certbot ausführst.

```bash
sudo certbot --nginx -d api.slate.jodegen.de -d slate.jodegen.de
```

Certbot passt die Nginx-Configs automatisch für HTTPS an und richtet Auto-Renewal ein.

### DNS-Einträge bei deinem Domain-Anbieter

Sobald du `jodegen.de` hast, folgende A-Records anlegen:

| Name | Typ | Wert |
|---|---|---|
| `slate` | A | `DEINE-HETZNER-IP` |
| `api.slate` | A | `DEINE-HETZNER-IP` |

### `.env` nach Nginx-Umstellung prüfen

```bash
nano ~/apps/slate/.env
```

Stelle sicher dass `CORS_ALLOWED_ORIGINS` auf die HTTPS-URL zeigt:

```env
CORS_ALLOWED_ORIGINS=https://slate.jodegen.de
```

```bash
docker compose up -d   # neu starten damit CORS greift
```


## 1. Server vorbereiten

```bash
# System updaten
sudo apt update && sudo apt upgrade -y

# Benötigte Tools installieren
sudo apt install -y git curl ufw
```

---

## 2. Docker installieren

```bash
# Docker GPG Key + Repository hinzufügen
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Docker ohne sudo nutzbar machen (danach neu einloggen!)
sudo usermod -aG docker $USER
newgrp docker

# Testen
docker --version
docker compose version
```

---

## 3. Firewall konfigurieren

```bash
sudo ufw allow OpenSSH
sudo ufw allow 8080/tcp   # API Port (oder 80/443 wenn Reverse Proxy)
sudo ufw enable
sudo ufw status
```

---

## 4. Projekt auf den Server bringen

```bash
# Projektverzeichnis anlegen
mkdir -p ~/apps/slate && cd ~/apps/slate

# Option A: Per Git (empfohlen)
git clone https://github.com/dein-username/slate-backend.git .

# Option B: Per SCP vom lokalen Rechner (von deinem Mac aus ausführen)
# scp -r /Users/jonasdegen/Desktop/develop/Slate/slate-backend user@DEINE-IP:~/apps/slate
```

---

## 5. Umgebungsvariablen konfigurieren

```bash
cd ~/apps/slate

# .env aus dem Beispiel erstellen
cp .env.example .env

# JWT Secret generieren
openssl rand -base64 64

# .env befüllen
nano .env
```

Inhalt der `.env`:

```env
DB_USER=slate
DB_PASSWORD=sicheres_passwort_hier

# Den generierten Base64-String von oben einfügen
JWT_SECRET=dein-generierter-base64-string

JWT_EXPIRY_MS=900000
JWT_REFRESH_EXPIRY_MS=2592000000

CORS_ALLOWED_ORIGINS=https://deine-frontend-domain.com
```

> **Hinweis:** `nano` speichern mit `Ctrl+O`, beenden mit `Ctrl+X`

---

## 6. Starten

```bash
# Image bauen und Stack starten
docker compose up --build -d

# Logs prüfen
docker compose logs -f app

# Status prüfen
docker compose ps
```

Die API ist jetzt unter `http://DEINE-SERVER-IP:8080` erreichbar.

---

## 7. Testen

```bash
# Health-Check — sollte 401 zurückgeben (API läuft)
curl -s http://localhost:8080/api/users/me

# Registrierung testen
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!","name":"Test User"}' | python3 -m json.tool
```

---

## 8. Nützliche Befehle

```bash
# Stack stoppen
docker compose down

# Stack stoppen + Datenbank löschen (VORSICHT!)
docker compose down -v

# Neu bauen und starten (nach Code-Änderungen)
docker compose up --build -d

# App-Logs live verfolgen
docker compose logs -f app

# In die Datenbank schauen
docker compose exec db psql -U slate -d slate

# Container-Status
docker compose ps
```

---

## 9. Updates deployen

```bash
cd ~/apps/slate

# Neuesten Code holen
git pull

# Neu bauen und starten (Downtime: ~30 Sek.)
docker compose up --build -d
```

---

## 10. Nginx als Reverse Proxy + HTTPS

Da das Frontend ebenfalls auf dem Server laufen wird, übernimmt Nginx das Routing für beide Domains:
- `api.slate.jodegen.de` → Backend (Port 8080)
- `slate.jodegen.de` → Frontend (Port 3000 oder wo auch immer das Frontend läuft)

### Nginx installieren

```bash
sudo apt install -y nginx certbot python3-certbot-nginx

# Port 80 + 443 in der Firewall freigeben
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw delete allow 8080/tcp   # Port 8080 nicht mehr direkt erreichbar
```

### Nginx-Config für das Backend

```bash
sudo nano /etc/nginx/sites-available/slate-api
```

```nginx
server {
    server_name api.slate.jodegen.de;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Nginx-Config für das Frontend

```bash
sudo nano /etc/nginx/sites-available/slate-frontend
```

```nginx
server {
    server_name slate.jodegen.de;

    location / {
        proxy_pass http://localhost:3000;   # Port deines Frontends anpassen
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Aktivieren & testen

```bash
sudo ln -s /etc/nginx/sites-available/slate-api /etc/nginx/sites-enabled/
sudo ln -s /etc/nginx/sites-available/slate-frontend /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

### SSL-Zertifikate ausstellen (Let's Encrypt)

> **Voraussetzung:** Beide DNS-Einträge müssen bereits auf die Server-IP zeigen, bevor du Certbot ausführst.

```bash
sudo certbot --nginx -d api.slate.jodegen.de -d slate.jodegen.de
```

Certbot passt die Nginx-Configs automatisch für HTTPS an und richtet Auto-Renewal ein.

### DNS-Einträge bei deinem Domain-Anbieter

Sobald du `jodegen.de` hast, folgende A-Records anlegen:

| Name | Typ | Wert |
|---|---|---|
| `slate` | A | `DEINE-HETZNER-IP` |
| `api.slate` | A | `DEINE-HETZNER-IP` |

### `.env` auf dem Server anpassen

```bash
nano ~/apps/slate/.env
```

```env
CORS_ALLOWED_ORIGINS=https://slate.jodegen.de
```

```bash
docker compose up -d   # neu starten damit CORS greift
```


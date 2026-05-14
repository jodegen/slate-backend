# Slate Backend

REST API for personal lifestyle tracking — weight, sleep, training, and daily routines.
Built with Spring Boot 4, PostgreSQL, and JWT authentication.

---

## Tech Stack

- **Java 21** + Spring Boot 4
- **PostgreSQL 16** via Spring Data JPA + Hibernate
- **Liquibase** for schema migrations
- **JWT** (jjwt 0.12) for stateless auth
- **MapStruct** for entity → DTO mapping
- **Docker + Docker Compose** for deployment

---

## Getting Started

### Prerequisites

- Docker + Docker Compose
- Java 21 (for local development)

### Run with Docker Compose

```bash
# 1. Copy the example env file and fill in your values
cp .env.example .env

# 2. Generate a secure JWT secret
openssl rand -base64 64

# 3. Start the stack
docker compose up --build -d

# API is available at http://localhost:8080
```

### Run Locally (without Docker)

Requires a running PostgreSQL instance.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/slate
export DB_USER=slate
export DB_PASSWORD=your_password
export JWT_SECRET=your-base64-secret
export CORS_ALLOWED_ORIGINS=http://localhost:3000

./mvnw spring-boot:run
```

---

## API Overview

| Module | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `/api/auth/login`, `/api/auth/refresh` |
| User | `GET /api/users/me` |
| Weight | `GET/POST /api/weights`, `PUT/DELETE /api/weights/{date}` |
| Sleep | `GET/POST /api/sleep`, `PUT/DELETE /api/sleep/{date}` |
| Routine | `GET/POST /api/routines`, `GET /api/routines/{date}`, `POST/PUT/DELETE /api/routines/{date}/items`, `DELETE /api/routines/{date}` |
| Training | `GET/POST /api/training`, `GET/DELETE /api/training/{date}`, sessions + sets endpoints |
| Health Import | `POST /api/health/import/steps`, `POST /api/health/import/sleep` |

All endpoints except `/api/auth/**` and `/api/health/import/**` require a `Bearer` token in the `Authorization` header.

---

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | JDBC URL for PostgreSQL | — |
| `DB_USER` | Database username | — |
| `DB_PASSWORD` | Database password | — |
| `JWT_SECRET` | Base64-encoded secret (min. 256-bit) | — |
| `JWT_EXPIRY_MS` | Access token TTL in ms | `900000` (15 min) |
| `JWT_REFRESH_EXPIRY_MS` | Refresh token TTL in ms | `2592000000` (30 days) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:3000` |
| `IMPORT_API_KEY` | Shared secret for the health import endpoints (sent as `X-Import-Key` header from iOS Shortcuts) | `dev-import-key` |

---

## Running Tests

```bash
./mvnw test
```

---

## Apple Health Import via iOS Shortcuts

Steps and sleep data can be sent automatically from your iPhone using the iOS **Shortcuts** app. See **[SHORTCUTS.md](./SHORTCUTS.md)** for the full payload spec and Shortcut configuration.

### Endpoints

| Endpoint | Data | Action |
|---|---|---|
| `POST /api/health/import/steps` | Hourly step counts for the day | Adds `"steps"` to `RoutineLog` if total ≥ 10,000 |
| `POST /api/health/import/sleep` | Sleep segments with phases | Upserts `SleepLog` with total non-Awake duration |

### Authentication

These endpoints do **not** require a JWT token. Instead, send the `X-Import-Key` header with your `IMPORT_API_KEY` value:

```
X-Import-Key: your-import-api-key
```

Set `IMPORT_API_KEY` in your `.env` file on the server. The default for local development is `dev-import-key`.

### Payload format

**Steps** (`POST /api/health/import/steps`):
```json
{
  "steps":     "18\n54\n0\n...\n0",
  "dateTimes": "2026-05-14T00:56:15+02:00\n...\n2026-05-15T00:00:00+02:00"
}
```

**Sleep** (`POST /api/health/import/sleep`):
```json
{
  "sleepStartTimes": "2026-04-19T02:34:38+02:00\n...",
  "sleepEndTimes":   "2026-04-19T02:48:08+02:00\n...\n2026-04-20T00:02:46+02:00",
  "sleepPhases":     "Core\nAwake\nCore\nDeep\n..."
}
```

Values are newline-separated strings as produced by the iOS Health API. See `SHORTCUTS.md` for the exact Shortcut configuration.

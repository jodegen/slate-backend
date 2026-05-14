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
| Health Import | `POST /api/health/import` |

All endpoints except `/api/auth/**` require a `Bearer` token in the `Authorization` header.

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

---

## Running Tests

```bash
./mvnw test
```

---

## Apple Health Import via iOS Shortcut

The `POST /api/health/import` endpoint accepts health data sent directly from your iPhone using the iOS **Shortcuts** app. This lets you automatically sync sleep, steps, and workouts from Apple Health to Slate.

### What gets imported

| Data | Action |
|---|---|
| **Sleep** | Sleep duration (Core + Deep + REM stages) is upserted into your sleep log |
| **Steps ≥ 10,000** | Adds `"steps"` to your routine log for that day |
| **Workouts** | Creates a training session on the matching training day |

### Building the Shortcut

Open the **Shortcuts** app on your iPhone and create a new shortcut with the following steps:

---

#### Step 1 — Set your API token

> **Action:** Text  
> **Content:** `your_access_token_here`  
> **Save as variable:** `authToken`

Paste the Bearer token you received from `POST /api/auth/login`.

---

#### Step 2 — Calculate the date window

> **Action:** Date  
> **→** Adjust Date: subtract **1 day**  
> **Save as variable:** `startDate`

This imports yesterday's data. Change to `7 days` for a weekly sync.

---

#### Step 3 — Fetch steps (daily total)

> **Action:** Find Health Samples  
> - Type: **Step Count**  
> - Start Date is after: `startDate`  
> - Group by: **Day**  
> **Save as variable:** `stepSamples`

> **Action:** Set Variable `stepList` = *(empty list — use a Text action with nothing in it)*

> **Action:** Repeat with Each `stepSamples`  
> Inside the loop:  
> &nbsp;&nbsp;**Action:** Dictionary  
> &nbsp;&nbsp;- `value` → Repeat Item → **Value**  
> &nbsp;&nbsp;- `date` → Repeat Item → **Start Date** → Format as ISO 8601  
> &nbsp;&nbsp;**Action:** Add to Variable `stepList`

---

#### Step 4 — Fetch sleep samples

> **Action:** Find Health Samples  
> - Type: **Sleep Analysis**  
> - Start Date is after: `startDate`  
> **Save as variable:** `sleepSamples`

> **Action:** Set Variable `sleepList` = *(empty)*

> **Action:** Repeat with Each `sleepSamples`  
> Inside the loop:  
> &nbsp;&nbsp;**Action:** Dictionary  
> &nbsp;&nbsp;- `sleepStage` → Repeat Item → **Value**  
> &nbsp;&nbsp;- `startDate` → Repeat Item → **Start Date** → Format as ISO 8601  
> &nbsp;&nbsp;- `endDate` → Repeat Item → **End Date** → Format as ISO 8601  
> &nbsp;&nbsp;- `durationSeconds` → Get Time Between Repeat Item Start Date and End Date (in seconds)  
> &nbsp;&nbsp;**Action:** Add to Variable `sleepList`

---

#### Step 5 — Fetch workouts

> **Action:** Find Health Samples  
> - Type: **Workout**  
> - Start Date is after: `startDate`  
> **Save as variable:** `workoutSamples`

> **Action:** Set Variable `workoutList` = *(empty)*

> **Action:** Repeat with Each `workoutSamples`  
> Inside the loop:  
> &nbsp;&nbsp;**Action:** Dictionary  
> &nbsp;&nbsp;- `workoutType` → Repeat Item → **Name**  
> &nbsp;&nbsp;- `durationSeconds` → Repeat Item → **Duration** (in seconds)  
> &nbsp;&nbsp;- `startDate` → Repeat Item → **Start Date** → Format as ISO 8601  
> &nbsp;&nbsp;- `endDate` → Repeat Item → **End Date** → Format as ISO 8601  
> &nbsp;&nbsp;**Action:** Add to Variable `workoutList`

---

#### Step 6 — Assemble payload and send

> **Action:** Dictionary  
> - `sentAt` → Current Date → Format as ISO 8601  
> - `steps` → `stepList`  
> - `sleep` → `sleepList`  
> - `workouts` → `workoutList`  
> **Save as variable:** `payload`

> **Action:** Get Contents of URL  
> - URL: `https://your-api.com/api/health/import`  
> - Method: **POST**  
> - Headers:  
> &nbsp;&nbsp;- `Authorization` → `Bearer ` + `authToken`  
> &nbsp;&nbsp;- `Content-Type` → `application/json`  
> - Request Body: **JSON** → `payload`

---

#### Step 7 — (Optional) Show result

> **Action:** Show Notification / Show Result  
> Content: Contents of URL (the response from the API)

---

### Automate the Shortcut

To run this shortcut automatically every morning:

1. Open the **Shortcuts** app → **Automation** tab
2. Tap **+** → **Time of Day** (e.g. 08:00)
3. Select **Run Shortcut** → choose your health sync shortcut
4. Disable "Ask Before Running" so it runs silently

> **Tip:** Run the shortcut manually once first to grant all HealthKit permissions (steps, sleep, workouts). iOS will ask for permission on the first run for each data type.

### Supported workout types

The following Apple workout types are automatically mapped to training categories:

| Apple Workout Type | Slate Category |
|---|---|
| Running | CARDIO |
| Cycling | CARDIO |
| Indoor Cycling | CARDIO |
| High Intensity Interval Training | CARDIO |
| Walking | CARDIO |
| Functional Strength Training | PUSH |
| Traditional Strength Training | PUSH |
| Core Training | PUSH |
| *(any other type)* | CARDIO (default) |

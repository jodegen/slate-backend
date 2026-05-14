# CLAUDE.md – Slate Backend

## Project Overview

**Slate** is a REST API for a personal lifestyle tracking app. Users can track weight, workouts,
sleep, and daily routines. Apple Health data is imported via iOS Shortcut as JSON.

- **Group ID / Root package:** `de.jodegen.slate`
- **Deployment target:** Docker + Docker Compose on a VServer

---

## Tech Stack

| Technology | Version |
|---|---|
| Java | 21 
| Spring Boot | 4.0.x |
| Spring Security + JWT | jjwt 0.12 |
| Spring Data JPA + Hibernate | — |
| PostgreSQL | 16 |
| Liquibase | — |
| Lombok | latest |
| MapStruct | 1.6.x |
| Docker + Docker Compose | — |

---

## Build & Run Commands

```bash
# Build (skip tests)
./mvnw clean package -DskipTests

# Run tests
./mvnw test

# Run locally (requires PostgreSQL running)
./mvnw spring-boot:run

# Start full stack via Docker Compose
docker compose up --build
```

---

## Package Structure

Root: `de.jodegen.slate`

```
de.jodegen.slate/
├── config/          # SecurityConfig, JwtConfig
├── common/          # Shared code: exception/, dto/
├── auth/            # Login, register, refresh — no CQRS split needed
├── user/            # User entity + GET /api/users/me
├── weight/          # WeightEntry — command/ + query/
├── training/        # TrainingDay, TrainingSession, ExerciseSet — command/ + query/
├── sleep/           # SleepLog — command/ + query/
├── routine/         # RoutineLog — command/ + query/
└── healthimport/    # Cross-cutting import — own service, no CQRS split
```

Each feature module follows the CQRS structure described below.

---

## Architecture Rules

- **Controllers** handle routing and request validation only — no business logic.
- **Command services** contain all write logic (create, update, delete).
- **Query services** contain all read logic (fetching, filtering, projecting to view DTOs).
- **Repositories** are pure Spring Data JPA interfaces — no custom query logic in controllers.
- **DTOs** are used for all request/response bodies. Never expose JPA entities directly in API responses.
- **User identity** is always resolved from the authenticated JWT principal — never from the request body.
- Global exception handling via `@ControllerAdvice` with a uniform error response:
  ```json
  { "error": "ERROR_CODE", "message": "Human-readable message", "status": 400 }
  ```

---

## CQRS Pattern

This project uses **structural CQRS** with a single database. Commands and queries are separated
at the service layer — no event sourcing, no separate read database.

### Naming Conventions

| Concern | Suffix | Example |
|---|---|---|
| Write operations | `CommandService` | `WeightCommandService` |
| Read operations | `QueryService` | `WeightQueryService` |
| Input for writes | `Command` | `CreateWeightCommand`, `UpdateWeightCommand` |
| Input for reads | `Query` | `WeightsByDateRangeQuery` |
| Read result DTO | `View` | `WeightEntryView`, `TrainingDayView` |
| Write result DTO | `Response` (or `View`) | `WeightEntryView` |

### Module Structure (per feature)

```
weight/
├── WeightEntry.java              # JPA Entity (write model)
├── WeightRepository.java
├── command/
│   ├── WeightCommandService.java # create, update, delete
│   ├── CreateWeightCommand.java  # input DTO for create
│   └── UpdateWeightCommand.java  # input DTO for update
└── query/
    ├── WeightQueryService.java   # fetch, filter
    ├── WeightsByDateRangeQuery.java  # optional: query parameter object
    └── WeightEntryView.java      # read result DTO
```

### Rules

- A `CommandService` method **never returns** domain data beyond a confirmation or the created/updated view.
- A `QueryService` method **never mutates** state.
- Controllers may call both a `CommandService` and a `QueryService` but must not mix write and read logic themselves.
- Command DTOs are validated with Bean Validation before reaching the service.
- View DTOs are plain records or Lombok `@Value` classes — immutable.

---

## Coding Conventions

- All code, comments, variable names, and documentation must be in **English**.
- Root package: `de.jodegen.slate`
- All entities use `UUID` as primary key (`@GeneratedValue` with `UUID` strategy).
- Use `@CreationTimestamp` for `createdAt` fields.
- Use Lombok: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` on entities and DTOs.
- Validate all incoming DTOs with Bean Validation (`@NotNull`, `@NotBlank`, `@Min`, etc.).
- Use `LocalDate` for dates, `LocalTime` for times (no `java.util.Date` or `Timestamp`).
- ENUMs are used for typed fields (e.g., `TrainingType`, `DataSource`).
- No raw `String` constants for enum-like values.
- Use **MapStruct** for all Entity → View mappings. Mappers live in the `query/` package of each module and are named `<Entity>Mapper` (e.g., `WeightEntryMapper`).
- Mappers are Spring components (`@Mapper(componentModel = "spring")`).
- Never map manually with `new ViewDto(entity.getX(), ...)` — always use a MapStruct mapper.

---

## Transaction Management

- `@Transactional` goes on **CommandService** methods (write operations).
- `@Transactional(readOnly = true)` goes on **QueryService** methods (read operations).
- Never place `@Transactional` on controllers or repositories.
- Keep transactions as short as possible — do not include external calls (e.g., HTTP) inside a transaction boundary.

---

## Exception Handling

Custom exceptions live in `de.jodegen.slate.common.exception`:

| Exception | HTTP Status | When to throw |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entity not found by ID or date |
| `ConflictException` | 409 | Unique constraint violation (e.g., duplicate date entry) |
| `ValidationException` | 400 | Business rule violation beyond Bean Validation |

- All exceptions are caught by a `@ControllerAdvice` (`GlobalExceptionHandler`) and mapped to:
  ```json
  { "error": "RESOURCE_NOT_FOUND", "message": "No weight entry for 2026-05-14", "status": 404 }
  ```
- The `error` field uses `SCREAMING_SNAKE_CASE` matching the exception type.
- Bean Validation errors (`MethodArgumentNotValidException`) are also handled in `GlobalExceptionHandler`.

---

## API Conventions

### HTTP Status Codes

| Operation | Status |
|---|---|
| GET (found) | 200 OK |
| POST (created) | 201 Created |
| PUT / upsert | 200 OK |
| DELETE | 204 No Content |
| Not found | 404 Not Found |
| Duplicate / conflict | 409 Conflict |
| Validation error | 400 Bad Request |
| Unauthorized | 401 Unauthorized |

### Date & Time Format

All dates and times in JSON use **ISO 8601**:
- `LocalDate` → `"2026-05-14"`
- `LocalTime` → `"08:15:00"`
- **Note:** `spring.jackson.serialization.write-dates-as-timestamps=false` is **invalid in Spring Boot 4 / Jackson 3** — do not use it. Jackson 3 handles `LocalDate` serialization correctly by default via the JavaTimeModule.

---

## Database Migrations (Liquibase)

- All schema changes are managed by **Liquibase** — never use `spring.jpa.hibernate.ddl-auto=create` or `update` in any environment.
- Set `spring.jpa.hibernate.ddl-auto=validate` in all environments.
- Migration files live in `src/main/resources/db/changelog/`.
- Master changelog: `db/changelog/db.changelog-master.yaml`
- Each migration is a separate numbered file: `db/changelog/changes/001-create-users.yaml`, `002-create-weight-entries.yaml`, etc.
- Never modify an already-applied migration — always add a new changeset.

---

## Security Rules

- **Never** hardcode secrets, passwords, or API keys in source code or config files.
- All secrets go into environment variables (`.env` file, never committed to version control).
- Required environment variables:
  ```
  DB_URL, DB_USER, DB_PASSWORD
  JWT_SECRET          # minimum 256-bit
  JWT_EXPIRY_MS       # access token TTL (default: 900000 = 15 min)
  JWT_REFRESH_EXPIRY_MS  # refresh token TTL (default: 2592000000 = 30 days)
  CORS_ALLOWED_ORIGINS
  ```
- All `/api/**` routes require a valid Bearer token except `/api/auth/**`.
- CORS allowed origins come from `CORS_ALLOWED_ORIGINS` env var — never hardcoded.
- No refresh token rotation required for now — static validation is sufficient.

---

## JWT Configuration

- Access token TTL: **15 minutes**
- Refresh token TTL: **30 days**
- Secret via env var `JWT_SECRET` (min. 256-bit / 32 chars)
- Library: `io.jsonwebtoken:jjwt` 0.12.x

---

## Health Import Logic

When `POST /api/health-import` is called:
1. **Sleep** → upsert `SleepLog` for the given date (`source = HEALTH_IMPORT`)
2. **Workout** → create `TrainingSession` (`source = HEALTH_IMPORT`)
   - If a `TrainingDay` already exists for that date → add session to it
   - If not → create a new `TrainingDay` with `plannedType` derived from the weekly schedule
3. **Steps ≥ 10,000** → add `"steps"` to `RoutineLog.completedItems` for that date
4. Response indicates what was created vs. updated

### Weekly Schedule (for `plannedType` auto-detection)
| Day | Type |
|---|---|
| Monday | PUSH |
| Tuesday | CARDIO |
| Wednesday | PULL |
| Thursday | CARDIO |
| Friday | PUSH |
| Saturday | CARDIO |
| Sunday | REST |

---

## Spring Security 7 Notes

- `DaoAuthenticationProvider` no longer has `setUserDetailsService()` — pass it via constructor:
  ```java
  new DaoAuthenticationProvider(userDetailsService)
  ```
- `ObjectMapper` is **not** available for direct injection in `SecurityConfig` (Jackson not in compile scope that way). Write inline JSON strings in `AuthenticationEntryPoint` lambdas.

---



- Test framework: **JUnit 5** (via Spring Boot test starters)
- **Controller tests:** Use `@WebMvcTest` with `MockMvc` — mock the service layer with Mockito.
- **Service tests:** Pure unit tests with Mockito — no Spring context needed.
- **Repository tests:** Use `@DataJpaTest` with H2 in-memory DB.
- Test class naming: `<ClassName>Test` (e.g., `WeightServiceTest`, `AuthControllerTest`).
- Each test method name should describe the scenario: `shouldReturnWeightEntry_whenValidRequest()`.
- Do not write tests for trivial getters/setters generated by Lombok.

### Spring Boot 4 Test Annotations (Breaking Changes)

Spring Boot 4 moved test annotations to new packages — **always use these imports:**

| Annotation | Import |
|---|---|
| `@WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| `@DataJpaTest` | `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` |
| `@MockitoBean` | `org.springframework.test.context.bean.override.mockito.MockitoBean` |
| `TestEntityManager` | `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager` |

- `@MockBean` was **removed** — use `@MockitoBean` instead.
- `spring-boot-starter-test` alone is **not sufficient** for slice tests; the following starters are required:
  - `spring-boot-starter-webmvc-test` for `@WebMvcTest`
  - `spring-boot-starter-data-jpa-test` for `@DataJpaTest`
  - `spring-boot-starter-security-test` for `SecurityMockMvcRequestPostProcessors`

### Controller Tests (`@WebMvcTest`)

- **CSRF:** `@WebMvcTest` enforces CSRF even when `csrf.disable()` is set in `SecurityConfig`. Always add `.with(csrf())` to POST/PUT/DELETE requests:
  ```java
  mockMvc.perform(post("/api/weights").with(user(mockUser)).with(csrf())...)
  ```
- The `User` domain class (implements `UserDetails`) can be used directly with `.with(user(mockUser))`.
- Mock these beans in every controller test (required by `SecurityConfig` and `JwtAuthenticationFilter`):
  ```java
  @MockitoBean JwtService jwtService;
  @MockitoBean UserRepository userRepository;
  @MockitoBean CustomUserDetailsService customUserDetailsService;
  ```

### Repository Tests (`@DataJpaTest`)

- Disable Liquibase and use Hibernate schema creation for H2:
  ```java
  @DataJpaTest
  @TestPropertySource(properties = {
      "spring.liquibase.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop"
  })
  ```

### Smoke Test (`SlateApplicationTests`)

- Full context test requires DB and JWT properties. Use H2 + Base64-encoded secret:
  ```java
  @SpringBootTest
  @TestPropertySource(properties = {
      "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.liquibase.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "jwt.secret=<base64-encoded-secret-min-32-bytes>",
      "cors.allowed-origins=http://localhost:3000"
  })
  ```
- **JWT secret in tests must be valid Base64** — `JwtService` decodes it with `Decoders.BASE64.decode()`. Avoid characters like `-` or `_` (use URL-safe Base64 or a pure alphanumeric string).

---

## Out of Scope (Do Not Implement)

- Push notifications
- Multiple training plans / plan management
- Statistics / aggregation endpoints (handled in the frontend)
- Admin panel

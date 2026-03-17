# Restaurant Table Recommendation

Backend + frontend project for recommending restaurant tables based on:
- current availability (occupied/free at a selected date and time),
- party size,
- user preferences (zone, window side, quiet area, accessibility, near play area).

The app returns:
- one `recommendedTable`,
- up to 3 `alternativeTables`,
- score and human-readable reasons for each suggestion.

## What The Project Does

- Stores restaurant layout (tables, coordinates, capacities, features).
- Stores reservations and calculates occupied tables for a specific date/time.
- Provides API endpoints for:
  - layout rendering,
  - availability snapshot,
  - recommendation search.
- Includes a React UI that:
  - displays an SVG layout,
  - runs search with preferences,
  - highlights recommended/alternative/occupied tables.
- Generates random reservations at backend startup (configurable).

## Stack

### Backend
- Java 25
- Spring Boot 4 (Web MVC, Validation, Data JPA)
- PostgreSQL
- Liquibase migrations + seed
- Gradle
- JUnit 5 + Mockito

### Frontend
- React 18
- TypeScript
- Vite
- Nginx (in Docker image)

### Infra
- Docker + Docker Compose

## Project Structure

- `backend/` - Spring Boot API
- `frontend/` - React client
- `docker-compose.yml` - full local environment (db + backend + frontend)

## API Overview

- `GET /api/health` - health check
- `GET /api/layout` - all tables for layout (without occupied status)
- `GET /api/tables?date=YYYY-MM-DD&time=HH:mm` - tables with `occupied` status
  - if params are missing, backend uses current date/time
- `POST /api/search` - recommendation search

Example request (`POST /api/search`):

```json
{
  "date": "2026-03-18",
  "time": "19:00",
  "partySize": 4,
  "zone": "MAIN",
  "windowSide": true,
  "quietArea": false,
  "accessible": true,
  "nearPlayArea": false
}
```

## How To Run Backend

### Prerequisites
- JDK 25+
- Docker (recommended for PostgreSQL)

### 1) Start PostgreSQL

From project root:

```bash
docker compose up -d db
```

### 2) Run backend locally

```bash
cd backend
./gradlew bootRun
```

PowerShell:

```powershell
cd backend
.\gradlew.bat bootRun
```

Backend URL: `http://localhost:8080`

## How To Run Frontend

### Prerequisites
- Node.js 20+

### Run locally

```bash
cd frontend
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

Notes:
- In dev mode Vite proxies `/api` to `http://localhost:8080`.
- Frontend can also use `VITE_API_BASE` if needed.

## How To Run With Docker

Run everything:

```bash
docker compose up -d --build
```

Services:
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Database: `localhost:5432`

Docker frontend (Nginx) proxies `/api` to backend container.

## Recommendation Logic

Search flow (`POST /api/search`):
1. Load availability snapshot for requested `date/time`.
2. Apply hard filters:
   - exclude occupied tables,
   - exclude tables with `capacity < partySize`.
3. Score remaining tables:
   - capacity fit (exact fit gets highest score),
   - zone match bonus/penalty,
   - preference bonuses/penalties:
     - window side,
     - quiet area,
     - accessible,
     - near play area.
4. Sort by:
   - score desc,
   - smaller capacity delta,
   - id asc.
5. Return:
   - first table as `recommendedTable`,
   - next 3 as `alternativeTables`,
   - `reasons` explaining score contributions.

### Important behavior
- `false == false` does not get the same bonus as `true == true`.
- Zone values: `MAIN`, `TERRACE`, `PRIVATE`.
- Unsupported zone returns `400 Bad Request`.

## Random Reservation Generation

At backend startup, random reservations are generated (enabled by default):
- reservation table is refreshed,
- reservations are generated for the next N days,
- no overlapping reservations for same table/time.

Config (`backend/src/main/resources/application.properties`):
- `app.seed.random-reservations.enabled`
- `app.seed.random-reservations.days`
- `app.seed.random-reservations.min-per-day`
- `app.seed.random-reservations.max-per-day`

Environment variable overrides are supported:
- `APP_RANDOM_RESERVATIONS_ENABLED`
- `APP_RANDOM_RESERVATIONS_DAYS`
- `APP_RANDOM_RESERVATIONS_MIN_PER_DAY`
- `APP_RANDOM_RESERVATIONS_MAX_PER_DAY`

## Assumptions

- Single-restaurant layout (not multi-tenant).
- Reservation status is computed at a single point in time (no full timeline planner yet).
- Request includes one `date` and one `time` for search.
- Scoring is heuristic (deterministic weighted rules), not ML-based.
- Frontend expects backend to be reachable at `/api` path (proxy/local setup).

## Current Limitations

- No authentication/authorization.
- No create/update/cancel reservation flow yet.
- No conflict-safe write endpoint for booking finalization.
- No pagination/filtering for large table counts.
- Time zone handling is basic (server-local behavior).
- Random startup seeding is convenient for demo but not production-safe by default.

## What Can Be Improved Next

- Add booking endpoint with transaction-safe conflict checks.
- Add admin APIs/UI for editing layout and table features.
- Add reservation timeline/calendar view.
- Add metrics/observability (structured logs, tracing, dashboards).
- Add integration tests with Testcontainers (PostgreSQL).
- Add CI pipeline with lint, tests, and build artifacts.
- Improve recommendation explainability and configurable scoring profiles.

## Time Spent

- Backend: ~22 hours
- Frontend: ~15 hours
- Total: ~37 hours

## Challenges And How They Were Solved

- Aligning backend and frontend contracts (`/api/tables` + `/api/search`) and keeping SVG highlighting consistent.
  Solved by introducing shared frontend types and deterministic matching rules for recommended/alternative tables.
- Liquibase + evolving domain rules (zone enum migration, seed consistency, warnings in changelog).
  Solved by explicit migration steps and normalizing zone values to `MAIN/TERRACE/PRIVATE`.
- Balancing recommendation quality and predictability.
  Solved by separating hard filters from scoring, adding fixed weights, capping alternatives to top 3, and returning human-readable reasons.
- Random reservation generation without invalid overlaps.
  Solved by startup seeder constraints: time-slot boundaries, no same-table overlap, party-size <= capacity.

## Help / References Used

- Official Spring Boot documentation (Web MVC, Validation, Data JPA testing patterns).
- Liquibase documentation (changeSet syntax and typed values in YAML).
- Mockito / JUnit 5 docs for unit and controller-level tests.
- React + Vite docs for proxying and API integration.
- IntelliJ inspections and local test feedback (`gradlew test`) to fix compile/runtime issues.

## Known Unresolved Issues And Proposed Fixes

- No booking write-flow yet (`POST /api/reservations` with conflict-safe transaction).
  Proposed fix: add reservation command endpoint with optimistic/pessimistic locking and overlap check at DB query level.
- Random startup seeding is demo-oriented and rewrites reservation data on app start.
  Proposed fix: enable only in `dev` profile and disable in `prod`, or seed only when table is empty.
- Time zone behavior is server-local for date/time evaluation.
  Proposed fix: define canonical timezone strategy (UTC in API + client-local conversions, or explicit timezone in request).
- Validation/API error format is basic.
  Proposed fix: add unified error response model (`code`, `message`, `fieldErrors`) via `@ControllerAdvice`.

## Tests

Run backend tests:

```bash
cd backend
./gradlew test
```

PowerShell:

```powershell
cd backend
.\gradlew.bat test
```

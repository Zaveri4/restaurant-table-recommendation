# restaurant-table-recommendation

Basic setup to run backend and frontend locally.

Prerequisites:
- JDK 25+
- Node.js 20+
- Docker (for Postgres)

Start everything with Docker:
1. `docker compose up -d --build`

Start only the database:
1. `docker compose up -d db`

Run the backend locally:
1. `cd backend`
2. `./gradlew bootRun` (PowerShell: `.\gradlew.bat bootRun`)

Run the frontend locally:
1. `cd frontend`
2. `npm install`
3. `npm run dev`

Health check endpoint: `http://localhost:8080/api/health`
Frontend via Docker: `http://localhost:5173`

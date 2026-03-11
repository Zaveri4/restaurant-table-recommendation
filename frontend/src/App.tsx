import { useEffect, useState } from "react";

type HealthResponse = {
  status: string;
  time: string;
};

const API_BASE = import.meta.env.VITE_API_BASE ?? "";

export default function App() {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetch(`${API_BASE}/api/health`)
      .then((response) => {
        if (!response.ok) {
          throw new Error(`Request failed: ${response.status}`);
        }
        return response.json();
      })
      .then((data: HealthResponse) => {
        if (!cancelled) {
          setHealth(data);
        }
      })
      .catch((err: Error) => {
        if (!cancelled) {
          setError(err.message);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="app">
      <header className="hero">
        <p className="eyebrow">Restaurant Table Recommendation</p>
        <h1>
          Smart seating decisions
          <span> start with a live backend.</span>
        </h1>
        <p className="subtitle">
          This starter verifies the backend status and gives you a clean,
          opinionated foundation to build on.
        </p>
        <div className="status-card">
          <div>
            <p className="label">Backend status</p>
            <p className="value">
              {health ? health.status : error ? "error" : "loading"}
            </p>
            <p className="meta">
              {health
                ? `Last check: ${health.time}`
                : error
                ? `Error: ${error}`
                : "Waiting for response from /api/health"}
            </p>
          </div>
          <div className="chip">:8080</div>
        </div>
      </header>
      <section className="grid">
        <article>
          <h2>Backend</h2>
          <p>
            Spring Boot on Java 21 with Postgres and Liquibase migrations. Use
            Docker for the database and run Gradle locally.
          </p>
        </article>
        <article>
          <h2>Frontend</h2>
          <p>
            Vite + React + TypeScript with an API proxy to the backend for quick
            iteration and zero CORS friction.
          </p>
        </article>
        <article>
          <h2>Next steps</h2>
          <p>
            Start modeling tables, reservations, and shifts, then enrich the
            UI with operational insights.
          </p>
        </article>
      </section>
    </div>
  );
}

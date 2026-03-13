import { useEffect, useState } from "react";
import { fetchLayout, type RestaurantTable } from "./api/layout";

const GRID_UNIT = 36;
const CANVAS_PADDING = 24;
const TABLE_FILL = "#0ea5e9";

export default function App() {
  const [tables, setTables] = useState<RestaurantTable[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchLayout()
      .then((data) => {
        if (!cancelled) {
          setTables(data);
          setLoading(false);
        }
      })
      .catch((err: Error) => {
        if (!cancelled) {
          setError(err.message);
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const maxX = tables.reduce(
    (maxValue, table) => Math.max(maxValue, table.xPosition + table.width),
    0
  );
  const maxY = tables.reduce(
    (maxValue, table) => Math.max(maxValue, table.yPosition + table.height),
    0
  );

  const svgWidth = Math.max(520, maxX * GRID_UNIT + CANVAS_PADDING * 2);
  const svgHeight = Math.max(360, maxY * GRID_UNIT + CANVAS_PADDING * 2);

  return (
    <div className="app">
      <header className="hero">
        <p className="eyebrow">Restaurant Table Recommendation</p>
        <h1>
          Restaurant layout
          <span> from backend /api/layout.</span>
        </h1>
        <p className="subtitle">
          Current table list for quick verification before building visual
          layout UI.
        </p>
        <div className="status-card">
          <div>
            <p className="label">Layout request</p>
            <p className="value">
              {loading ? "loading" : error ? "error" : "ok"}
            </p>
            <p className="meta">
              {error
                ? `Error: ${error}`
                : loading
                ? "Waiting for response from /api/layout"
                : `Loaded ${tables.length} tables`}
            </p>
          </div>
          <div className="chip">:8080</div>
        </div>
      </header>
      <section className="layout-card">
        <div className="layout-head">
          <h2>SVG Layout</h2>
          <div className="legend">
            <span className="legend-swatch" />
            <span>Restaurant table</span>
          </div>
        </div>
        {!loading && !error && tables.length === 0 && (
          <p className="empty-state">No tables found.</p>
        )}
        {tables.length > 0 && (
          <div className="layout-canvas">
            <svg
              className="layout-svg"
              viewBox={`0 0 ${svgWidth} ${svgHeight}`}
              role="img"
              aria-label="Restaurant tables layout"
            >
              {tables.map((table) => {
                const x = table.xPosition * GRID_UNIT + CANVAS_PADDING;
                const y = table.yPosition * GRID_UNIT + CANVAS_PADDING;
                const width = table.width * GRID_UNIT;
                const height = table.height * GRID_UNIT;
                const centerX = x + width / 2;
                const centerY = y + height / 2;

                return (
                  <g key={table.id}>
                    <rect
                      x={x}
                      y={y}
                      width={width}
                      height={height}
                      rx={8}
                      fill={TABLE_FILL}
                      className="table-rect"
                    />
                    <text x={centerX} y={centerY - 8} className="table-name">
                      {table.name}
                    </text>
                    <text x={centerX} y={centerY + 12} className="table-meta">
                      {table.capacity} ppl
                    </text>
                  </g>
                );
              })}
            </svg>
          </div>
        )}
      </section>
    </div>
  );
}

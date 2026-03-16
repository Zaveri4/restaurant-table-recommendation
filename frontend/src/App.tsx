import { useEffect, useState } from "react";
import { fetchTables } from "./api/tables";
import { searchTables } from "./api/search";
import type {
  RestaurantTableStatus,
  RestaurantZone,
  SearchResult,
  SearchTable,
  SearchTablesRequest,
} from "./types";

const GRID_UNIT = 36;
const CANVAS_PADDING = 24;
const RECOMMENDED_FILL = "#16a34a";
const ALTERNATIVE_FILL = "#f59e0b";
const AVAILABLE_FILL = "#cbd5e1";
const OCCUPIED_FILL = "#ef4444";
type TableVisualState = "recommended" | "alternative" | "occupied" | "available";

type BooleanPreference = "ANY" | "TRUE" | "FALSE";

type SearchFormState = {
  date: string;
  time: string;
  partySize: number;
  zone: "" | RestaurantZone;
  windowSide: BooleanPreference;
  quietArea: BooleanPreference;
  accessible: BooleanPreference;
  nearPlayArea: BooleanPreference;
};

const now = new Date();
const defaultDate = now.toISOString().slice(0, 10);
const defaultTime = now.toTimeString().slice(0, 5);

const initialFormState: SearchFormState = {
  date: defaultDate,
  time: defaultTime,
  partySize: 2,
  zone: "",
  windowSide: "ANY",
  quietArea: "ANY",
  accessible: "ANY",
  nearPlayArea: "ANY",
};

function toOptionalBoolean(value: BooleanPreference): boolean | undefined {
  if (value === "TRUE") {
    return true;
  }
  if (value === "FALSE") {
    return false;
  }
  return undefined;
}

function getTableFill(
  table: RestaurantTableStatus,
  recommendedTableId: number | null,
  alternativeTableIds: Set<number>
): string {
  if (table.occupied) {
    return OCCUPIED_FILL;
  }
  if (table.id === recommendedTableId) {
    return RECOMMENDED_FILL;
  }
  if (alternativeTableIds.has(table.id)) {
    return ALTERNATIVE_FILL;
  }
  return AVAILABLE_FILL;
}

function getTableVisualState(
  table: RestaurantTableStatus,
  recommendedTableId: number | null,
  alternativeTableIds: Set<number>
): TableVisualState {
  if (table.occupied) {
    return "occupied";
  }
  if (table.id === recommendedTableId) {
    return "recommended";
  }
  if (alternativeTableIds.has(table.id)) {
    return "alternative";
  }
  return "available";
}

function normalizeId(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string" && value.trim() !== "") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return null;
}

function resolveTableId(
  searchTable: SearchTable | null | undefined,
  tables: RestaurantTableStatus[]
): number | null {
  if (!searchTable) {
    return null;
  }

  const numericId = normalizeId(searchTable.id);
  if (numericId !== null && tables.some((table) => table.id === numericId)) {
    return numericId;
  }

  const matchedByName = tables.find((table) => table.name === searchTable.name);
  return matchedByName?.id ?? null;
}

export default function App() {
  const [tables, setTables] = useState<RestaurantTableStatus[]>([]);
  const [searchResult, setSearchResult] = useState<SearchResult | null>(null);
  const [form, setForm] = useState<SearchFormState>(initialFormState);
  const [loadingTables, setLoadingTables] = useState(true);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [snapshotLabel, setSnapshotLabel] = useState("Current moment");

  useEffect(() => {
    let cancelled = false;

    fetchTables()
      .then((data) => {
        if (!cancelled) {
          setTables(data);
          setLoadingTables(false);
        }
      })
      .catch((requestError: Error) => {
        if (!cancelled) {
          setError(requestError.message);
          setLoadingTables(false);
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

  const recommendedTableId = resolveTableId(
    searchResult?.recommendedTable,
    tables
  );
  const alternativeTableIds = new Set(
    (searchResult?.alternativeTables ?? [])
      .map((table) => resolveTableId(table, tables))
      .filter((id): id is number => id !== null)
  );

  async function handleSearchSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSearching(true);
    setSearchError(null);

    const payload: SearchTablesRequest = {
      date: form.date,
      time: form.time,
      partySize: form.partySize,
      zone: form.zone || undefined,
      windowSide: toOptionalBoolean(form.windowSide),
      quietArea: toOptionalBoolean(form.quietArea),
      accessible: toOptionalBoolean(form.accessible),
      nearPlayArea: toOptionalBoolean(form.nearPlayArea),
    };

    try {
      const [result, tableSnapshot] = await Promise.all([
        searchTables(payload),
        fetchTables(form.date, form.time),
      ]);
      setSearchResult(result);
      setTables(tableSnapshot);
      setSnapshotLabel(`Snapshot: ${form.date} ${form.time}`);
    } catch (requestError) {
      const message =
        requestError instanceof Error ? requestError.message : "Search failed";
      setSearchError(message);
    } finally {
      setSearching(false);
    }
  }

  function renderResultCard() {
    if (!searchResult) {
      return (
        <p className="empty-state">
          Submit search request to get recommended table and alternatives.
        </p>
      );
    }

    return (
      <>
        <h3>Recommended table</h3>
        {searchResult.recommendedTable ? (
          <article className="result-card recommended">
            <p className="result-title">{searchResult.recommendedTable.name}</p>
            <p className="result-meta">
              Capacity: {searchResult.recommendedTable.capacity} | Score:{" "}
              {searchResult.recommendedTable.score}
            </p>
            <ul>
              {searchResult.recommendedTable.reasons.map((reason) => (
                <li key={reason}>{reason}</li>
              ))}
            </ul>
          </article>
        ) : (
          <p className="empty-state">No recommended table for selected query.</p>
        )}

        <h3>Alternative tables</h3>
        {searchResult.alternativeTables.length > 0 ? (
          <div className="alternative-grid">
            {searchResult.alternativeTables.map((table) => (
              <article key={table.id} className="result-card alternative">
                <p className="result-title">{table.name}</p>
                <p className="result-meta">
                  Capacity: {table.capacity} | Score: {table.score}
                </p>
                <ul>
                  {table.reasons.map((reason) => (
                    <li key={reason}>{reason}</li>
                  ))}
                </ul>
              </article>
            ))}
          </div>
        ) : (
          <p className="empty-state">No alternatives found.</p>
        )}
      </>
    );
  }

  return (
    <div className="app">
      <header className="hero">
        <p className="eyebrow">Restaurant Table Recommendation</p>
        <h1>
          Search and visualize
          <span> table recommendation on live layout.</span>
        </h1>
        <p className="subtitle">
          Frontend combines current table state from GET /api/tables and ranking
          response from POST /api/search.
        </p>
        <div className="status-card">
          <div>
            <p className="label">Tables request</p>
            <p className="value">
              {loadingTables ? "loading" : error ? "error" : "ok"}
            </p>
            <p className="meta">
              {error
                ? `Error: ${error}`
                : loadingTables
                ? "Waiting for response from /api/tables"
                : `${tables.length} tables loaded - ${snapshotLabel}`}
            </p>
          </div>
          <div className="chip">:8080</div>
        </div>
      </header>

      <section className="workspace-grid">
        <article className="search-card">
          <h2>Search tables</h2>
          <form className="search-form" onSubmit={handleSearchSubmit}>
            <label>
              Date
              <input
                type="date"
                value={form.date}
                onChange={(event) =>
                  setForm((current) => ({ ...current, date: event.target.value }))
                }
                required
              />
            </label>
            <label>
              Time
              <input
                type="time"
                value={form.time}
                onChange={(event) =>
                  setForm((current) => ({ ...current, time: event.target.value }))
                }
                required
              />
            </label>
            <label>
              Party size
              <input
                type="number"
                min={1}
                max={20}
                value={form.partySize}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    partySize: Number(event.target.value),
                  }))
                }
                required
              />
            </label>
            <label>
              Zone preference
              <select
                value={form.zone}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    zone: event.target.value as SearchFormState["zone"],
                  }))
                }
              >
                <option value="">Any</option>
                <option value="MAIN">MAIN</option>
                <option value="TERRACE">TERRACE</option>
                <option value="PRIVATE">PRIVATE</option>
              </select>
            </label>
            <label>
              Window side
              <select
                value={form.windowSide}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    windowSide: event.target.value as BooleanPreference,
                  }))
                }
              >
                <option value="ANY">Any</option>
                <option value="TRUE">Yes</option>
                <option value="FALSE">No</option>
              </select>
            </label>
            <label>
              Quiet area
              <select
                value={form.quietArea}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    quietArea: event.target.value as BooleanPreference,
                  }))
                }
              >
                <option value="ANY">Any</option>
                <option value="TRUE">Yes</option>
                <option value="FALSE">No</option>
              </select>
            </label>
            <label>
              Accessible
              <select
                value={form.accessible}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    accessible: event.target.value as BooleanPreference,
                  }))
                }
              >
                <option value="ANY">Any</option>
                <option value="TRUE">Yes</option>
                <option value="FALSE">No</option>
              </select>
            </label>
            <label>
              Near play area
              <select
                value={form.nearPlayArea}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    nearPlayArea: event.target.value as BooleanPreference,
                  }))
                }
              >
                <option value="ANY">Any</option>
                <option value="TRUE">Yes</option>
                <option value="FALSE">No</option>
              </select>
            </label>
            <button type="submit" disabled={searching || loadingTables}>
              {searching ? "Searching..." : "Find table"}
            </button>
          </form>

          {searchError && <p className="search-error">Search error: {searchError}</p>}

          <div className="search-result">{renderResultCard()}</div>
        </article>

        <article className="layout-card">
          <div className="layout-head">
            <h2>Restaurant layout</h2>
            <div className="legend-grid">
              <div className="legend">
                <span className="legend-swatch legend-recommended" />
                <span>Recommended</span>
              </div>
              <div className="legend">
                <span className="legend-swatch legend-alternative" />
                <span>Alternative</span>
              </div>
              <div className="legend">
                <span className="legend-swatch legend-available" />
                <span>Available</span>
              </div>
              <div className="legend">
                <span className="legend-swatch legend-occupied" />
                <span>Occupied</span>
              </div>
            </div>
          </div>

          {!loadingTables && !error && tables.length === 0 && (
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
                  const state = getTableVisualState(
                    table,
                    recommendedTableId,
                    alternativeTableIds
                  );
                  const fill = getTableFill(
                    table,
                    recommendedTableId,
                    alternativeTableIds
                  );

                  return (
                    <g key={table.id}>
                      <rect
                        x={x}
                        y={y}
                        width={width}
                        height={height}
                        rx={8}
                        fill={fill}
                        className={`table-rect table-rect-${state}`}
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
        </article>
      </section>
    </div>
  );
}

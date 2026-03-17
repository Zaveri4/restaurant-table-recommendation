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

type FeatureKey = "windowSide" | "quietArea" | "accessible" | "nearPlayArea";

const FEATURE_LABELS: Record<FeatureKey, string> = {
  windowSide: "Window side",
  quietArea: "Quiet area",
  accessible: "Accessible",
  nearPlayArea: "Near play area",
};

const now = new Date();
const defaultDate = [
  now.getFullYear(),
  String(now.getMonth() + 1).padStart(2, "0"),
  String(now.getDate()).padStart(2, "0"),
].join("-");
const defaultTime = [
  String(now.getHours()).padStart(2, "0"),
  String(now.getMinutes()).padStart(2, "0"),
].join(":");

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

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

function getFeatureLabels(table: RestaurantTableStatus): string[] {
  const features: string[] = [];
  if (table.windowSide) {
    features.push(FEATURE_LABELS.windowSide);
  }
  if (table.quietArea) {
    features.push(FEATURE_LABELS.quietArea);
  }
  if (table.accessible) {
    features.push(FEATURE_LABELS.accessible);
  }
  if (table.nearPlayArea) {
    features.push(FEATURE_LABELS.nearPlayArea);
  }
  return features;
}

export default function App() {
  const [tables, setTables] = useState<RestaurantTableStatus[]>([]);
  const [searchResult, setSearchResult] = useState<SearchResult | null>(null);
  const [form, setForm] = useState<SearchFormState>(initialFormState);
  const [loadingLayout, setLoadingLayout] = useState(true);
  const [searching, setSearching] = useState(false);
  const [layoutError, setLayoutError] = useState<string | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [hasSearchAttempt, setHasSearchAttempt] = useState(false);
  const [snapshotLabel, setSnapshotLabel] = useState("Current moment");

  async function loadLayoutSnapshot(date?: string, time?: string) {
    setLoadingLayout(true);
    setLayoutError(null);
    try {
      const data = await fetchTables(date, time);
      setTables(data);
      setSnapshotLabel(date && time ? `Snapshot: ${date} ${time}` : "Current moment");
    } catch (requestError) {
      setLayoutError(toErrorMessage(requestError, "Layout request failed"));
    } finally {
      setLoadingLayout(false);
    }
  }

  useEffect(() => {
    void loadLayoutSnapshot();
  }, []);

  function handleRetryLayout() {
    if (hasSearchAttempt) {
      void loadLayoutSnapshot(form.date, form.time);
      return;
    }
    void loadLayoutSnapshot();
  }

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
  const occupiedCount = tables.filter((table) => table.occupied).length;
  const availableCount = Math.max(0, tables.length - occupiedCount);
  const topRecommendationName = searchResult?.recommendedTable?.name ?? null;

  async function handleSearchSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setHasSearchAttempt(true);
    setSearching(true);
    setSearchError(null);
    setLayoutError(null);
    setLoadingLayout(true);

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

    const [searchResultState, tableSnapshotState] = await Promise.allSettled([
      searchTables(payload),
      fetchTables(form.date, form.time),
    ]);

    if (searchResultState.status === "fulfilled") {
      setSearchResult(searchResultState.value);
    } else {
      setSearchResult(null);
      setSearchError(toErrorMessage(searchResultState.reason, "Search request failed"));
    }

    if (tableSnapshotState.status === "fulfilled") {
      setTables(tableSnapshotState.value);
      setSnapshotLabel(`Snapshot: ${form.date} ${form.time}`);
      setLayoutError(null);
    } else {
      setLayoutError(
        toErrorMessage(tableSnapshotState.reason, "Failed to load table availability")
      );
    }

    setLoadingLayout(false);
    setSearching(false);
  }

  function getTableContext(searchTable: SearchTable): RestaurantTableStatus | null {
    const tableId = resolveTableId(searchTable, tables);
    if (tableId === null) {
      return null;
    }
    const table = tables.find((candidate) => candidate.id === tableId);
    return table ?? null;
  }

  function renderSearchTableCard(searchTable: SearchTable, variant: "recommended" | "alternative") {
    const tableContext = getTableContext(searchTable);
    const features = tableContext ? getFeatureLabels(tableContext) : [];

    return (
      <article className={`result-card ${variant}`}>
        <div className="result-head">
          <p className="result-title">{searchTable.name}</p>
          <p className="result-score">Score {searchTable.score}</p>
        </div>
        <p className="result-meta">
          Capacity: {searchTable.capacity}
          {tableContext ? ` | Zone: ${tableContext.zone}` : ""}
        </p>
        {features.length > 0 && (
          <div className="result-tags">
            {features.map((feature) => (
              <span key={feature} className="result-tag">
                {feature}
              </span>
            ))}
          </div>
        )}
        <ul>
          {searchTable.reasons.map((reason, index) => (
            <li key={`${searchTable.id}-${index}-${reason}`}>{reason}</li>
          ))}
        </ul>
      </article>
    );
  }

  function renderResultCard() {
    if (searching) {
      return (
        <div className="state-card loading">
          <p className="state-title">Searching for best tables...</p>
          <p className="state-text">Request in progress: /api/search and /api/tables.</p>
        </div>
      );
    }

    if (searchError) {
      return (
        <div className="state-card error">
          <p className="state-title">Search request failed</p>
          <p className="state-text">{searchError}</p>
        </div>
      );
    }

    if (!hasSearchAttempt) {
      return (
        <div className="state-card initial">
          <p className="state-title">Ready to search</p>
          <p className="state-text">
            Choose date, time and preferences. Recommendation will appear here.
          </p>
        </div>
      );
    }

    if (
      !searchResult ||
      (!searchResult.recommendedTable &&
        searchResult.alternativeTables.length === 0)
    ) {
      return (
        <div className="state-card empty">
          <p className="state-title">No suitable tables</p>
          <p className="state-text">
            Try another time slot, smaller party size, or fewer strict preferences.
          </p>
        </div>
      );
    }

    return (
      <>
        <h3>Recommended table</h3>
        {searchResult.recommendedTable ? (
          renderSearchTableCard(searchResult.recommendedTable, "recommended")
        ) : (
          <div className="state-card empty">
            <p className="state-title">No recommended table</p>
            <p className="state-text">There are no options matching your current criteria.</p>
          </div>
        )}

        <h3>Alternative tables</h3>
        {searchResult.alternativeTables.length > 0 ? (
          <div className="alternative-grid">
            {searchResult.alternativeTables.map((table) => (
              <div key={table.id}>{renderSearchTableCard(table, "alternative")}</div>
            ))}
          </div>
        ) : (
          <div className="state-card empty">
            <p className="state-title">No alternatives</p>
            <p className="state-text">Recommended table is the only matching option.</p>
          </div>
        )}
      </>
    );
  }

  return (
    <div className="app">
      <header className="hero">
        <p className="eyebrow">Table Finder</p>
        <h1>
          Find the right table in seconds
          <span>Live availability and smart recommendations in one view.</span>
        </h1>
        <p className="subtitle">
          Select date, time and guest preferences. We highlight the best match and
          clear alternatives directly on the floor layout.
        </p>
        <div className="status-card">
          <div className="status-main">
            <p className="label">Live hall status</p>
            <p className="value">
              {loadingLayout
                ? "Updating availability..."
                : layoutError
                ? "Connection issue"
                : "Ready for booking"}
            </p>
            <p className="meta">
              {layoutError
                ? `Could not refresh table state: ${layoutError}`
                : loadingLayout
                ? "Refreshing table status from backend"
                : `${snapshotLabel}${topRecommendationName ? ` - Top pick: ${topRecommendationName}` : ""}`}
            </p>
          </div>
          <div className="hero-stats" aria-label="Summary">
            <div className="hero-stat">
              <span className="hero-stat-label">Total tables</span>
              <strong className="hero-stat-value">{tables.length}</strong>
            </div>
            <div className="hero-stat">
              <span className="hero-stat-label">Available</span>
              <strong className="hero-stat-value">{availableCount}</strong>
            </div>
            <div className="hero-stat">
              <span className="hero-stat-label">Occupied</span>
              <strong className="hero-stat-value">{occupiedCount}</strong>
            </div>
          </div>
        </div>
      </header>

      <section className="workspace-grid">
        <article className="search-card">
          <div className="section-head">
            <h2>Search tables</h2>
            <p className="section-subtitle">
              Set preferences and get best table recommendation with explanations.
            </p>
          </div>
          <form className="search-form" onSubmit={handleSearchSubmit}>
            <label>
              <span className="field-label">Date</span>
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
              <span className="field-label">Time</span>
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
              <span className="field-label">Party size</span>
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
              <span className="field-label">Zone preference</span>
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
              <span className="field-label">Window side</span>
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
              <span className="field-label">Quiet area</span>
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
              <span className="field-label">Accessible</span>
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
              <span className="field-label">Near play area</span>
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
            <button type="submit" disabled={searching || loadingLayout}>
              {searching ? "Searching..." : "Find table"}
            </button>
          </form>
          <div className="search-result">{renderResultCard()}</div>
        </article>

        <article className="layout-card">
          <div className="layout-head">
            <div className="section-head">
              <h2>Restaurant layout</h2>
              <p className="section-subtitle">{snapshotLabel}</p>
            </div>
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

          {loadingLayout && (
            <div className="state-card loading">
              <p className="state-title">Loading layout...</p>
              <p className="state-text">Fetching table state from /api/tables.</p>
            </div>
          )}

          {!loadingLayout && layoutError && (
            <div className="state-card error">
              <p className="state-title">Layout is unavailable</p>
              <p className="state-text">{layoutError}</p>
              <button type="button" className="secondary-button" onClick={handleRetryLayout}>
                Retry
              </button>
            </div>
          )}

          {!loadingLayout && !layoutError && tables.length === 0 && (
            <div className="state-card empty">
              <p className="state-title">Layout is empty</p>
              <p className="state-text">No tables were returned by backend.</p>
            </div>
          )}

          {!loadingLayout && !layoutError && tables.length > 0 && (
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

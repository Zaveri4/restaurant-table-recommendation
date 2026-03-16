import type { SearchResult, SearchTablesRequest } from "../types";
const API_BASE = import.meta.env.VITE_API_BASE ?? "";

export async function searchTables(
  payload: SearchTablesRequest
): Promise<SearchResult> {
  const response = await fetch(`${API_BASE}/api/search`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(`Search request failed: ${response.status}`);
  }

  return response.json() as Promise<SearchResult>;
}

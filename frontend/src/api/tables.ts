import type { RestaurantTableStatus } from "../types";
const API_BASE = import.meta.env.VITE_API_BASE ?? "";

export async function fetchTables(
  date?: string,
  time?: string
): Promise<RestaurantTableStatus[]> {
  const params = new URLSearchParams();
  if (date) {
    params.set("date", date);
  }
  if (time) {
    params.set("time", time);
  }

  const query = params.toString();
  const url = query ? `${API_BASE}/api/tables?${query}` : `${API_BASE}/api/tables`;

  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Failed to fetch tables: ${response.status}`);
  }
  return response.json() as Promise<RestaurantTableStatus[]>;
}

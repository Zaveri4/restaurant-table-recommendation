const API_BASE = import.meta.env.VITE_API_BASE ?? "";

export type RestaurantTable = {
  id: number;
  name: string;
  capacity: number;
  zone: "MAIN" | "QUIET" | "WINDOW";
  xPosition: number;
  yPosition: number;
  width: number;
  height: number;
  windowSide: boolean;
  quietArea: boolean;
  accessible: boolean;
  nearPlayArea: boolean;
};

const RETRY_ATTEMPTS = 10;
const RETRY_DELAY_MS = 1_000;

export async function fetchLayout(): Promise<RestaurantTable[]> {
  let lastError: Error | null = null;

  for (let attempt = 1; attempt <= RETRY_ATTEMPTS; attempt += 1) {
    try {
      const response = await fetch(`${API_BASE}/api/layout`);
      if (!response.ok) {
        throw new Error(`Failed to fetch layout: ${response.status}`);
      }
      return response.json() as Promise<RestaurantTable[]>;
    } catch (error) {
      lastError =
        error instanceof Error ? error : new Error("Failed to fetch layout");
      if (attempt < RETRY_ATTEMPTS) {
        await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY_MS));
      }
    }
  }

  throw lastError ?? new Error("Failed to fetch layout");
}

import type { RestaurantZone } from "./restaurant-table";

export type SearchTablesRequest = {
  date: string;
  time: string;
  partySize: number;
  zone?: RestaurantZone;
  windowSide?: boolean;
  quietArea?: boolean;
  accessible?: boolean;
  nearPlayArea?: boolean;
};

export type SearchTable = {
  id: number;
  name: string;
  capacity: number;
  occupied: boolean;
  score: number;
  reasons: string[];
};

export type SearchResult = {
  recommendedTable: SearchTable | null;
  alternativeTables: SearchTable[];
};

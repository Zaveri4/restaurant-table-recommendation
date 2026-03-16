export type RestaurantZone = "MAIN" | "TERRACE" | "PRIVATE";

export type RestaurantTable = {
  id: number;
  name: string;
  capacity: number;
  zone: RestaurantZone;
  xPosition: number;
  yPosition: number;
  width: number;
  height: number;
  windowSide: boolean;
  quietArea: boolean;
  accessible: boolean;
  nearPlayArea: boolean;
};

export type RestaurantTableStatus = RestaurantTable & {
  occupied: boolean;
};

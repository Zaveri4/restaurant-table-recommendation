package com.example.restaurant.dto;

import com.example.restaurant.entity.RestaurantZone;

public record RestaurantTableAvailabilityDto (
        Long id,
        String name,
        Integer capacity,
        RestaurantZone zone,
        Integer xPosition,
        Integer yPosition,
        Integer width,
        Integer height,
        Boolean windowSide,
        Boolean quietArea,
        Boolean accessible,
        Boolean nearPlayArea,
        Boolean occupied
) {
}

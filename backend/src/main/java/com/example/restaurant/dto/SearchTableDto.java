package com.example.restaurant.dto;

import java.util.List;

public record SearchTableDto(
        Long id,
        String name,
        Integer capacity,
        Boolean occupied,
        Integer score,
        List<String> reasons
) {
}

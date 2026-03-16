package com.example.restaurant.dto;

import java.util.List;

public record SearchResultDto(
        SearchTableDto recommendedTable,
        List<SearchTableDto> alternativeTables
) {
}

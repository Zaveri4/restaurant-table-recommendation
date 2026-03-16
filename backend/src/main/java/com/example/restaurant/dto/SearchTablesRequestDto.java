package com.example.restaurant.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;

public record SearchTablesRequestDto(
        @NotNull LocalDate date,
        @NotNull LocalTime time,
        @NotNull @Positive Integer partySize,
        String zone,
        Boolean windowSide,
        @JsonAlias("quiteArea") Boolean quietArea,
        Boolean accessible,
        Boolean nearPlayArea
) {
}

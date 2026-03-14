package com.example.restaurant.search;

import com.example.restaurant.availability.AvailabilityService;
import com.example.restaurant.dto.RestaurantTableAvailabilityDto;
import com.example.restaurant.dto.SearchResultDto;
import com.example.restaurant.dto.SearchTableDto;
import com.example.restaurant.dto.SearchTablesRequestDto;
import com.example.restaurant.entity.RestaurantZone;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private static final int ZONE_MATCH_SCORE = 8;
    private static final int WINDOW_MATCH_SCORE = 8;
    private static final int QUIET_MATCH_SCORE = 8;
    private static final int ACCESSIBLE_MATCH_SCORE = 6;
    private static final int PLAY_AREA_MATCH_SCORE = 15;

    private final AvailabilityService availabilityService;

    public SearchService(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    public SearchResultDto searchTables(SearchTablesRequestDto request) {
        RestaurantZone requestedZone = normalizeZone(request.zone());

        List<ScoredTable> scoredTables = availabilityService
                .getTablesWithAvailability(request.date(), request.time())
                .stream()
                .filter(table -> !Boolean.TRUE.equals(table.occupied()))
                .filter(table -> table.capacity() >= request.partySize())
                .map(table -> scoreTable(table, request, requestedZone))
                .sorted(Comparator.comparingInt(ScoredTable::score).reversed()
                        .thenComparingInt(ScoredTable::capacityDelta)
                        .thenComparing(scored -> scored.table().id()))
                .toList();

        if (scoredTables.isEmpty()) {
            return new SearchResultDto(null, List.of());
        }

        SearchTableDto recommendedTable = toDto(scoredTables.getFirst());
        List<SearchTableDto> alternatives = scoredTables.stream()
                .skip(1)
                .limit(3)
                .map(this::toDto)
                .toList();

        return new SearchResultDto(recommendedTable, alternatives);
    }

    private ScoredTable scoreTable(
            RestaurantTableAvailabilityDto table,
            SearchTablesRequestDto request,
            RestaurantZone requestedZone
    ) {
        int score = 0;
        int capacityDelta = table.capacity() - request.partySize();
        List<String> reasons = new ArrayList<>();

        score += capacityScore(capacityDelta, reasons);
        score += zoneScore(requestedZone, table, reasons);
        score += preferenceScore(
                request.windowSide(),
                table.windowSide(),
                WINDOW_MATCH_SCORE,
                "Matches window preference",
                reasons
        );
        score += preferenceScore(
                request.quietArea(),
                table.quietArea(),
                QUIET_MATCH_SCORE,
                "Matches quiet area preference",
                reasons
        );
        score += preferenceScore(
                request.accessible(),
                table.accessible(),
                ACCESSIBLE_MATCH_SCORE,
                "Accessible table",
                reasons
        );
        score += preferenceScore(
                request.nearPlayArea(),
                table.nearPlayArea(),
                PLAY_AREA_MATCH_SCORE,
                "Matches play area preference",
                reasons
        );

        int normalizedScore = Math.clamp(score, 0, 100);
        return new ScoredTable(table, normalizedScore, List.copyOf(reasons), capacityDelta);
    }

    private int capacityScore(int capacityDelta, List<String> reasons) {
        if (capacityDelta == 0) {
            reasons.add("Fits party size well");
            return 50;
        }
        if (capacityDelta == 1) {
            reasons.add("Close fit for party size");
            return 42;
        }
        if (capacityDelta == 2) {
            reasons.add("Slightly larger than needed");
            return 34;
        }

        reasons.add("Larger table than needed");
        return 26 - Math.min(18, (capacityDelta - 2) * 4);
    }

    private int zoneScore(
            RestaurantZone requestedZone,
            RestaurantTableAvailabilityDto table,
            List<String> reasons
    ) {
        if (requestedZone == null) {
            return 0;
        }
        if (requestedZone == table.zone()) {
            reasons.add("Located in selected zone");
            return ZONE_MATCH_SCORE;
        }
        return -ZONE_MATCH_SCORE;
    }

    private int preferenceScore(
            Boolean requested,
            Boolean actual,
            int matchScore,
            String matchReason,
            List<String> reasons
    ) {
        if (requested == null) {
            return 0;
        }
        if (Boolean.TRUE.equals(requested) && Boolean.TRUE.equals(actual)) {
            reasons.add(matchReason);
            return matchScore;
        }
        if (Boolean.FALSE.equals(requested) && Boolean.FALSE.equals(actual)) {
            return 0;
        }
        return -matchScore;
    }

    private SearchTableDto toDto(ScoredTable scoredTable) {
        RestaurantTableAvailabilityDto table = scoredTable.table();
        return new SearchTableDto(
                table.id(),
                table.name(),
                table.capacity(),
                table.occupied(),
                scoredTable.score(),
                scoredTable.reasons()
        );
    }

    private RestaurantZone normalizeZone(String zone) {
        if (zone == null || zone.isBlank()) {
            return null;
        }

        String normalized = zone
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (normalized) {
            case "MAIN" -> RestaurantZone.MAIN;
            case "TERRACE" -> RestaurantZone.TERRACE;
            case "PRIVATE" -> RestaurantZone.PRIVATE;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported zone value: " + zone + ". Allowed: MAIN, TERRACE, PRIVATE."
            );
        };

    }

    private record ScoredTable(
            RestaurantTableAvailabilityDto table,
            int score,
            List<String> reasons,
            int capacityDelta
    ) {
    }
}

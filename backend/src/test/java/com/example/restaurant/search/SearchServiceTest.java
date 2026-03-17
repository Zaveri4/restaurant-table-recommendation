package com.example.restaurant.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.restaurant.availability.AvailabilityService;
import com.example.restaurant.dto.RestaurantTableAvailabilityDto;
import com.example.restaurant.dto.SearchResultDto;
import com.example.restaurant.dto.SearchTableDto;
import com.example.restaurant.dto.SearchTablesRequestDto;
import com.example.restaurant.entity.RestaurantZone;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private SearchService searchService;

    @Test
    void occupiedTableIsExcludedFromResults() {
        SearchTablesRequestDto request = request(4, null, null, null, null, null);
        RestaurantTableAvailabilityDto occupied = table(1L, "T01", 4, RestaurantZone.MAIN, true, false, false, true, false);
        RestaurantTableAvailabilityDto available = table(2L, "T02", 4, RestaurantZone.MAIN, false, false, false, true, false);

        when(availabilityService.getTablesWithAvailability(any(), any())).thenReturn(List.of(occupied, available));

        SearchResultDto result = searchService.searchTables(request);

        assertThat(result.recommendedTable()).isNotNull();
        assertThat(result.recommendedTable().id()).isEqualTo(2L);
        assertThat(result.alternativeTables()).isEmpty();
    }

    @Test
    void tablesWithCapacityLessThanPartySizeAreExcluded() {
        SearchTablesRequestDto request = request(4, null, null, null, null, null);
        RestaurantTableAvailabilityDto tooSmall = table(1L, "T01", 2, RestaurantZone.MAIN, false, false, false, true, false);
        RestaurantTableAvailabilityDto fit = table(2L, "T02", 4, RestaurantZone.MAIN, false, false, false, true, false);

        when(availabilityService.getTablesWithAvailability(any(), any())).thenReturn(List.of(tooSmall, fit));

        SearchResultDto result = searchService.searchTables(request);

        assertThat(result.recommendedTable()).isNotNull();
        assertThat(result.recommendedTable().id()).isEqualTo(2L);
    }

    @Test
    void exactCapacityMatchWinsOverMuchLargerTable() {
        SearchTablesRequestDto request = request(4, null, null, null, null, null);
        RestaurantTableAvailabilityDto exact = table(1L, "T01", 4, RestaurantZone.MAIN, false, false, false, true, false);
        RestaurantTableAvailabilityDto larger = table(2L, "T02", 8, RestaurantZone.MAIN, false, false, false, true, false);

        when(availabilityService.getTablesWithAvailability(any(), any())).thenReturn(List.of(larger, exact));

        SearchResultDto result = searchService.searchTables(request);

        assertThat(result.recommendedTable()).isNotNull();
        assertThat(result.recommendedTable().id()).isEqualTo(1L);
        assertThat(result.recommendedTable().reasons()).contains("Fits party size well");
    }

    @Test
    void zonePreferenceAffectsScore() {
        SearchTablesRequestDto request = request(4, "TERRACE", null, null, null, null);
        RestaurantTableAvailabilityDto main = table(1L, "T01", 4, RestaurantZone.MAIN, false, false, false, true, false);
        RestaurantTableAvailabilityDto terrace = table(2L, "T02", 4, RestaurantZone.TERRACE, false, false, false, true, false);

        when(availabilityService.getTablesWithAvailability(any(), any())).thenReturn(List.of(main, terrace));

        SearchResultDto result = searchService.searchTables(request);

        assertThat(result.recommendedTable()).isNotNull();
        assertThat(result.recommendedTable().id()).isEqualTo(2L);
        assertThat(result.recommendedTable().reasons()).contains("Located in selected zone");
    }

    @Test
    void accessibleTrueGivesPriorityToAccessibleTable() {
        SearchTablesRequestDto request = request(4, null, null, null, true, null);
        RestaurantTableAvailabilityDto nonAccessible = table(1L, "T01", 4, RestaurantZone.MAIN, false, false, false, false, false);
        RestaurantTableAvailabilityDto accessible = table(2L, "T02", 4, RestaurantZone.MAIN, false, false, false, true, false);

        when(availabilityService.getTablesWithAvailability(any(), any())).thenReturn(List.of(nonAccessible, accessible));

        SearchResultDto result = searchService.searchTables(request);

        assertThat(result.recommendedTable()).isNotNull();
        assertThat(result.recommendedTable().id()).isEqualTo(2L);
        assertThat(result.recommendedTable().reasons()).contains("Accessible table");
    }

    @Test
    void returnsNullRecommendedTableWhenNoCandidatesFound() {
        SearchTablesRequestDto request = request(4, null, null, null, null, null);
        RestaurantTableAvailabilityDto occupied = table(1L, "T01", 4, RestaurantZone.MAIN, true, false, false, true, false);
        RestaurantTableAvailabilityDto tooSmall = table(2L, "T02", 2, RestaurantZone.MAIN, false, false, false, true, false);

        when(availabilityService.getTablesWithAvailability(any(), any())).thenReturn(List.of(occupied, tooSmall));

        SearchResultDto result = searchService.searchTables(request);

        assertThat(result.recommendedTable()).isNull();
        assertThat(result.alternativeTables()).isEmpty();
    }

    @Test
    void alternativesAreLimitedToTopThree() {
        SearchTablesRequestDto request = request(4, null, null, null, null, null);
        RestaurantTableAvailabilityDto t1 = table(1L, "T01", 4, RestaurantZone.MAIN, false, false, false, true, false);
        RestaurantTableAvailabilityDto t2 = table(2L, "T02", 5, RestaurantZone.MAIN, false, false, false, true, false);
        RestaurantTableAvailabilityDto t3 = table(3L, "T03", 6, RestaurantZone.MAIN, false, false, false, true, false);
        RestaurantTableAvailabilityDto t4 = table(4L, "T04", 7, RestaurantZone.MAIN, false, false, false, true, false);
        RestaurantTableAvailabilityDto t5 = table(5L, "T05", 8, RestaurantZone.MAIN, false, false, false, true, false);

        when(availabilityService.getTablesWithAvailability(any(), any())).thenReturn(List.of(t1, t2, t3, t4, t5));

        SearchResultDto result = searchService.searchTables(request);

        assertThat(result.recommendedTable()).isNotNull();
        assertThat(result.recommendedTable().id()).isEqualTo(1L);
        assertThat(result.alternativeTables()).hasSize(3);
        assertThat(result.alternativeTables())
                .extracting(SearchTableDto::id)
                .containsExactly(2L, 3L, 4L);
    }

    @Test
    void falsePreferenceDoesNotGiveSameBonusAsTruePreference() {
        SearchTablesRequestDto request = request(4, null, null, null, false, null);
        RestaurantTableAvailabilityDto accessible = table(1L, "T01", 4, RestaurantZone.MAIN, false, false, false, true, false);
        RestaurantTableAvailabilityDto nonAccessible = table(2L, "T02", 4, RestaurantZone.MAIN, false, false, false, false, false);

        when(availabilityService.getTablesWithAvailability(any(), any())).thenReturn(List.of(accessible, nonAccessible));

        SearchResultDto result = searchService.searchTables(request);

        assertThat(result.recommendedTable()).isNotNull();
        assertThat(result.recommendedTable().id()).isEqualTo(2L);
        assertThat(result.recommendedTable().reasons()).doesNotContain("Accessible table");
    }

    @Test
    void unsupportedZoneValueReturnsBadRequest() {
        SearchTablesRequestDto request = request(4, "INVALID_ZONE", null, null, null, null);

        assertThatThrownBy(() -> searchService.searchTables(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseException = (ResponseStatusException) ex;
                    assertThat(responseException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseException.getReason()).contains("Unsupported zone value");
                });
    }

    private SearchTablesRequestDto request(
            Integer partySize,
            String zone,
            Boolean windowSide,
            Boolean quietArea,
            Boolean accessible,
            Boolean nearPlayArea
    ) {
        return new SearchTablesRequestDto(
                LocalDate.of(2026, 3, 16),
                LocalTime.of(19, 0),
                partySize,
                zone,
                windowSide,
                quietArea,
                accessible,
                nearPlayArea
        );
    }

    private RestaurantTableAvailabilityDto table(
            Long id,
            String name,
            int capacity,
            RestaurantZone zone,
            boolean occupied,
            boolean windowSide,
            boolean quietArea,
            boolean accessible,
            boolean nearPlayArea
    ) {
        return new RestaurantTableAvailabilityDto(
                id,
                name,
                capacity,
                zone,
                0,
                0,
                1,
                1,
                windowSide,
                quietArea,
                accessible,
                nearPlayArea,
                occupied
        );
    }
}

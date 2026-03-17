package com.example.restaurant.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.restaurant.dto.RestaurantTableAvailabilityDto;
import com.example.restaurant.entity.Reservation;
import com.example.restaurant.entity.RestaurantTable;
import com.example.restaurant.entity.RestaurantZone;
import com.example.restaurant.repository.ReservationRepository;
import com.example.restaurant.repository.RestaurantTableRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvailabilityServiceTest {

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    @Test
    void marksTableAsOccupiedWhenReservationOverlapsRequestedTime() {
        LocalDate date = LocalDate.of(2026, 3, 17);
        LocalTime time = LocalTime.of(18, 30);

        RestaurantTable table = table(1L, "T01", 4, RestaurantZone.MAIN);
        Reservation reservation = reservation(table, LocalTime.of(18, 0), LocalTime.of(19, 0));

        when(restaurantTableRepository.findAll(Sort.by(Sort.Direction.ASC, "id")))
                .thenReturn(List.of(table));
        when(reservationRepository.findAllByReservationDate(date))
                .thenReturn(List.of(reservation));

        List<RestaurantTableAvailabilityDto> result =
                availabilityService.getTablesWithAvailability(date, time);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().occupied()).isTrue();
    }

    @Test
    void doesNotMarkTableAsOccupiedWhenRequestedTimeEqualsReservationEnd() {
        LocalDate date = LocalDate.of(2026, 3, 17);
        LocalTime time = LocalTime.of(19, 0);

        RestaurantTable table = table(1L, "T01", 4, RestaurantZone.MAIN);
        Reservation reservation = reservation(table, LocalTime.of(18, 0), LocalTime.of(19, 0));

        when(restaurantTableRepository.findAll(Sort.by(Sort.Direction.ASC, "id")))
                .thenReturn(List.of(table));
        when(reservationRepository.findAllByReservationDate(date))
                .thenReturn(List.of(reservation));

        List<RestaurantTableAvailabilityDto> result =
                availabilityService.getTablesWithAvailability(date, time);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().occupied()).isFalse();
    }

    @Test
    void requestsReservationsForProvidedDateAndMapsTableFieldsToDto() {
        LocalDate date = LocalDate.of(2026, 3, 18);
        LocalTime time = LocalTime.of(20, 0);

        RestaurantTable table = table(7L, "T07", 6, RestaurantZone.TERRACE);
        when(restaurantTableRepository.findAll(Sort.by(Sort.Direction.ASC, "id")))
                .thenReturn(List.of(table));
        when(reservationRepository.findAllByReservationDate(date))
                .thenReturn(List.of());

        List<RestaurantTableAvailabilityDto> result =
                availabilityService.getTablesWithAvailability(date, time);

        verify(reservationRepository).findAllByReservationDate(date);
        assertThat(result).hasSize(1);
        RestaurantTableAvailabilityDto dto = result.getFirst();
        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("T07");
        assertThat(dto.capacity()).isEqualTo(6);
        assertThat(dto.zone()).isEqualTo(RestaurantZone.TERRACE);
        assertThat(dto.xPosition()).isEqualTo(3);
        assertThat(dto.yPosition()).isEqualTo(4);
        assertThat(dto.width()).isEqualTo(2);
        assertThat(dto.height()).isEqualTo(2);
        assertThat(dto.windowSide()).isTrue();
        assertThat(dto.quietArea()).isFalse();
        assertThat(dto.accessible()).isTrue();
        assertThat(dto.nearPlayArea()).isFalse();
        assertThat(dto.occupied()).isFalse();
    }

    private RestaurantTable table(Long id, String name, Integer capacity, RestaurantZone zone) {
        RestaurantTable table = org.mockito.Mockito.mock(RestaurantTable.class);
        when(table.getId()).thenReturn(id);
        when(table.getName()).thenReturn(name);
        when(table.getCapacity()).thenReturn(capacity);
        when(table.getZone()).thenReturn(zone);
        when(table.getXPosition()).thenReturn(3);
        when(table.getYPosition()).thenReturn(4);
        when(table.getWidth()).thenReturn(2);
        when(table.getHeight()).thenReturn(2);
        when(table.getWindowSide()).thenReturn(true);
        when(table.getQuietArea()).thenReturn(false);
        when(table.getAccessible()).thenReturn(true);
        when(table.getNearPlayArea()).thenReturn(false);
        return table;
    }

    private Reservation reservation(RestaurantTable table, LocalTime start, LocalTime end) {
        Reservation reservation = org.mockito.Mockito.mock(Reservation.class);
        when(reservation.getRestaurantTable()).thenReturn(table);
        when(reservation.getStartTime()).thenReturn(start);
        when(reservation.getEndTime()).thenReturn(end);
        return reservation;
    }
}

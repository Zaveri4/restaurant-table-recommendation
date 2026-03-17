package com.example.restaurant.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.restaurant.entity.Reservation;
import com.example.restaurant.entity.RestaurantTable;
import com.example.restaurant.repository.ReservationRepository;
import com.example.restaurant.repository.RestaurantTableRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RandomReservationSeederTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @InjectMocks
    private RandomReservationSeeder seeder;

    @Test
    void skipsGenerationWhenSeederIsDisabled() {
        ReflectionTestUtils.setField(seeder, "enabled", false);

        seeder.run(new DefaultApplicationArguments());

        verify(restaurantTableRepository, never()).findAll();
        verify(reservationRepository, never()).deleteAllInBatch();
        verify(reservationRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void generatesRandomReservationsWithinConfiguredConstraints() {
        ReflectionTestUtils.setField(seeder, "enabled", true);
        ReflectionTestUtils.setField(seeder, "daysToGenerate", 2);
        ReflectionTestUtils.setField(seeder, "minReservationsPerDay", 2);
        ReflectionTestUtils.setField(seeder, "maxReservationsPerDay", 2);

        RestaurantTable t1 = table(1L, 2);
        RestaurantTable t2 = table(2L, 4);
        RestaurantTable t3 = table(3L, 6);
        when(restaurantTableRepository.findAll()).thenReturn(List.of(t1, t2, t3));

        LocalDate beforeRun = LocalDate.now();
        seeder.run(new DefaultApplicationArguments());
        LocalDate afterRun = LocalDate.now();

        verify(reservationRepository).deleteAllInBatch();
        ArgumentCaptor<List<Reservation>> savedCaptor =
                (ArgumentCaptor<List<Reservation>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(reservationRepository).saveAll(savedCaptor.capture());
        List<Reservation> saved = savedCaptor.getValue();

        assertThat(saved).hasSize(4);
        assertThat(saved)
                .allSatisfy(reservation -> {
                    assertThat(reservation.getReservationDate())
                            .isBetween(beforeRun, afterRun.plusDays(1));
                    assertThat(reservation.getStartTime())
                            .isBetween(LocalTime.of(17, 0), LocalTime.of(21, 30));
                    assertThat(reservation.getEndTime()).isAfter(reservation.getStartTime());
                    assertThat(reservation.getEndTime()).isBeforeOrEqualTo(LocalTime.of(23, 0));
                    assertThat(reservation.getPartySize())
                            .isBetween(1, reservation.getRestaurantTable().getCapacity());
                });

        assertNoOverlapsPerTableAndDate(saved);
    }

    private void assertNoOverlapsPerTableAndDate(List<Reservation> reservations) {
        Map<String, List<Reservation>> grouped = reservations.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        reservation -> reservation.getRestaurantTable().getId()
                                + "|" + reservation.getReservationDate()
                ));

        for (List<Reservation> group : grouped.values()) {
            List<Reservation> sorted = group.stream()
                    .sorted(Comparator.comparing(Reservation::getStartTime))
                    .toList();

            for (int i = 1; i < sorted.size(); i++) {
                Reservation previous = sorted.get(i - 1);
                Reservation current = sorted.get(i);
                assertThat(previous.getEndTime().isAfter(current.getStartTime())).isFalse();
            }
        }
    }

    private RestaurantTable table(Long id, Integer capacity) {
        RestaurantTable table = org.mockito.Mockito.mock(RestaurantTable.class);
        when(table.getId()).thenReturn(id);
        when(table.getCapacity()).thenReturn(capacity);
        return table;
    }
}

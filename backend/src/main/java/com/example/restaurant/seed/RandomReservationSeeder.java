package com.example.restaurant.seed;

import com.example.restaurant.entity.Reservation;
import com.example.restaurant.entity.RestaurantTable;
import com.example.restaurant.repository.ReservationRepository;
import com.example.restaurant.repository.RestaurantTableRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomReservationSeeder implements ApplicationRunner {

    private static final LocalTime OPENING_TIME = LocalTime.of(17, 0);
    private static final LocalTime LAST_START_TIME = LocalTime.of(21, 30);
    private static final LocalTime CLOSING_TIME = LocalTime.of(23, 0);
    private static final List<Integer> ALLOWED_DURATIONS_MINUTES = List.of(60, 90, 120);
    private static final int SLOT_STEP_MINUTES = 30;
    private static final int MAX_GENERATION_ATTEMPTS_PER_DAY = 300;

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository restaurantTableRepository;

    @Value("${app.seed.random-reservations.enabled:true}")
    private boolean enabled;

    @Value("${app.seed.random-reservations.days:2}")
    private int daysToGenerate;

    @Value("${app.seed.random-reservations.min-per-day:3}")
    private int minReservationsPerDay;

    @Value("${app.seed.random-reservations.max-per-day:5}")
    private int maxReservationsPerDay;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (!enabled) {
            log.info("Random reservation seeding is disabled");
            return;
        }

        List<RestaurantTable> tables = restaurantTableRepository.findAll();
        if (tables.isEmpty()) {
            log.warn("Skipping random reservation seeding: no tables found");
            return;
        }

        reservationRepository.deleteAllInBatch();

        int safeDays = Math.max(1, daysToGenerate);
        int safeMinPerDay = Math.max(1, minReservationsPerDay);
        int safeMaxPerDay = Math.max(safeMinPerDay, maxReservationsPerDay);

        LocalDate startDate = LocalDate.now();
        List<Reservation> generatedReservations = new ArrayList<>();
        for (int offset = 0; offset < safeDays; offset++) {
            LocalDate date = startDate.plusDays(offset);
            generatedReservations.addAll(
                    generateReservationsForDate(date, tables, safeMinPerDay, safeMaxPerDay)
            );
        }

        reservationRepository.saveAll(generatedReservations);
        log.info(
                "Generated {} random reservations for {} day(s)",
                generatedReservations.size(),
                safeDays
        );
    }

    private List<Reservation> generateReservationsForDate(
            LocalDate date,
            List<RestaurantTable> tables,
            int minPerDay,
            int maxPerDay
    ) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int targetCount = random.nextInt(minPerDay, maxPerDay + 1);
        Map<Long, List<TimeRange>> tableSchedules = new HashMap<>();
        List<Reservation> generated = new ArrayList<>();

        int attempts = 0;
        while (generated.size() < targetCount && attempts < MAX_GENERATION_ATTEMPTS_PER_DAY) {
            attempts++;

            RestaurantTable table = tables.get(random.nextInt(tables.size()));
            LocalTime start = randomStartTime(random);
            int durationMinutes = ALLOWED_DURATIONS_MINUTES.get(
                    random.nextInt(ALLOWED_DURATIONS_MINUTES.size())
            );
            LocalTime end = start.plusMinutes(durationMinutes);
            if (end.isAfter(CLOSING_TIME)) {
                continue;
            }

            List<TimeRange> reservedSlots = tableSchedules.computeIfAbsent(
                    table.getId(),
                    key -> new ArrayList<>()
            );

            if (hasOverlap(reservedSlots, start, end)) {
                continue;
            }

            Reservation reservation = Reservation.of(
                    table,
                    date,
                    start,
                    end,
                    randomPartySize(random, table.getCapacity())
            );

            generated.add(reservation);
            reservedSlots.add(new TimeRange(start, end));
        }

        return generated;
    }

    private LocalTime randomStartTime(ThreadLocalRandom random) {
        int slotCount = (int) ((LAST_START_TIME.toSecondOfDay() - OPENING_TIME.toSecondOfDay())
                / (SLOT_STEP_MINUTES * 60L)) + 1;
        int selectedSlot = random.nextInt(slotCount);
        return OPENING_TIME.plusMinutes((long) selectedSlot * SLOT_STEP_MINUTES);
    }

    private int randomPartySize(ThreadLocalRandom random, int capacity) {
        int minSize = Math.min(2, capacity);
        return random.nextInt(minSize, capacity + 1);
    }

    private boolean hasOverlap(List<TimeRange> reservedSlots, LocalTime start, LocalTime end) {
        for (TimeRange reserved : reservedSlots) {
            if (start.isBefore(reserved.end()) && end.isAfter(reserved.start())) {
                return true;
            }
        }
        return false;
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }
}

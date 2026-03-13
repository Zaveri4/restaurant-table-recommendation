package com.example.restaurant.availability;

import com.example.restaurant.dto.RestaurantTableAvailabilityDto;
import com.example.restaurant.entity.Reservation;
import com.example.restaurant.entity.RestaurantTable;
import com.example.restaurant.repository.ReservationRepository;
import com.example.restaurant.repository.RestaurantTableRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    private final RestaurantTableRepository restaurantTableRepository;
    private final ReservationRepository reservationRepository;

    public AvailabilityService(
            RestaurantTableRepository restaurantTableRepository,
            ReservationRepository reservationRepository
    ) {
        this.restaurantTableRepository = restaurantTableRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<RestaurantTableAvailabilityDto> getTablesWithAvailability(LocalDate date, LocalTime time) {
        List<RestaurantTable> tables = restaurantTableRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<Reservation> reservations = reservationRepository.findAllByReservationDate(date);

        Set<Long> occupiedTableIds = reservations.stream()
                .filter(reservation -> isOccupiedAt(reservation, time))
                .map(reservation -> reservation.getRestaurantTable().getId())
                .collect(java.util.stream.Collectors.toSet());

        return tables.stream()
                .map(table -> toDto(table, occupiedTableIds.contains(table.getId())))
                .toList();
    }

    private boolean isOccupiedAt(Reservation reservation, LocalTime time) {
        return !time.isBefore(reservation.getStartTime()) && time.isBefore(reservation.getEndTime());
    }

    private RestaurantTableAvailabilityDto toDto(RestaurantTable table, boolean occupied) {
        return new RestaurantTableAvailabilityDto(
                table.getId(),
                table.getName(),
                table.getCapacity(),
                table.getZone(),
                table.getXPosition(),
                table.getYPosition(),
                table.getWidth(),
                table.getHeight(),
                table.getWindowSide(),
                table.getQuietArea(),
                table.getAccessible(),
                table.getNearPlayArea(),
                occupied
        );
    }
}

package com.example.restaurant.repository;

import com.example.restaurant.entity.Reservation;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findAllByReservationDate(LocalDate reservationDate);

    List<Reservation> findAllByReservationDateAndRestaurantTable_Id(LocalDate reservationDate, Long tableId);

    List<Reservation> findAllByReservationDateAndStartTimeLessThanAndEndTimeGreaterThan(
            LocalDate reservationDate,
            LocalTime endTime,
            LocalTime startTime
    );
}

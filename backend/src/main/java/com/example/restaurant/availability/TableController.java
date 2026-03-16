package com.example.restaurant.availability;

import com.example.restaurant.dto.RestaurantTableAvailabilityDto;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    private final AvailabilityService availabilityService;

    public TableController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public List<RestaurantTableAvailabilityDto> getTables(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalTime targetTime = time != null ? time : LocalTime.now();
        return availabilityService.getTablesWithAvailability(targetDate, targetTime);
    }
}

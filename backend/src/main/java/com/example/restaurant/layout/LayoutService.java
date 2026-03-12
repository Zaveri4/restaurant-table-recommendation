package com.example.restaurant.layout;

import com.example.restaurant.dto.RestaurantTableDto;
import com.example.restaurant.entity.RestaurantTable;
import com.example.restaurant.repository.RestaurantTableRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LayoutService {

    private final RestaurantTableRepository restaurantTableRepository;

    public LayoutService(RestaurantTableRepository restaurantTableRepository) {
        this.restaurantTableRepository = restaurantTableRepository;
    }

    public List<RestaurantTableDto> getLayoutTables() {
        return restaurantTableRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private RestaurantTableDto toDto(RestaurantTable table) {
        return new RestaurantTableDto(
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
                table.getNearPlayArea()
        );
    }
}

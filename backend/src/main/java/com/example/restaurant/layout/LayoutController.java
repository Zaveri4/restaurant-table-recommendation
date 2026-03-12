package com.example.restaurant.layout;

import com.example.restaurant.dto.RestaurantTableDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/layout")
public class LayoutController {

    private final LayoutService layoutService;

    public LayoutController(LayoutService layoutService) {
        this.layoutService = layoutService;
    }

    @GetMapping
    public List<RestaurantTableDto> getLayout() {
        return layoutService.getLayoutTables();
    }
}

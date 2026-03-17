package com.example.restaurant.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurant.dto.RestaurantTableAvailabilityDto;
import com.example.restaurant.entity.RestaurantZone;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TableControllerTest {

    private AvailabilityService availabilityService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        availabilityService = Mockito.mock(AvailabilityService.class);
        TableController controller = new TableController(availabilityService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void returnsAvailabilityForProvidedDateAndTime() throws Exception {
        LocalDate date = LocalDate.of(2026, 3, 17);
        LocalTime time = LocalTime.of(19, 0);
        RestaurantTableAvailabilityDto dto = new RestaurantTableAvailabilityDto(
                1L,
                "T01",
                4,
                RestaurantZone.MAIN,
                1,
                1,
                2,
                2,
                true,
                false,
                true,
                false,
                true
        );
        when(availabilityService.getTablesWithAvailability(date, time)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/tables")
                        .param("date", "2026-03-17")
                        .param("time", "19:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("T01"))
                .andExpect(jsonPath("$[0].zone").value("MAIN"))
                .andExpect(jsonPath("$[0].occupied").value(true));

        verify(availabilityService).getTablesWithAvailability(date, time);
    }

    @Test
    void usesCurrentDateAndTimeWhenRequestParamsAreNotProvided() throws Exception {
        when(availabilityService.getTablesWithAvailability(
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalTime.class)
        )).thenReturn(List.of());

        mockMvc.perform(get("/api/tables"))
                .andExpect(status().isOk());

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalTime> timeCaptor = ArgumentCaptor.forClass(LocalTime.class);
        verify(availabilityService).getTablesWithAvailability(dateCaptor.capture(), timeCaptor.capture());

        assertThat(dateCaptor.getValue()).isNotNull();
        assertThat(timeCaptor.getValue()).isNotNull();
    }
}

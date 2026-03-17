package com.example.restaurant.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurant.dto.SearchResultDto;
import com.example.restaurant.dto.SearchTableDto;
import com.example.restaurant.dto.SearchTablesRequestDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class SearchControllerTest {

    private SearchService searchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        searchService = Mockito.mock(SearchService.class);
        SearchController controller = new SearchController(searchService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    void returnsSearchResultForValidRequest() throws Exception {
        SearchResultDto response = new SearchResultDto(
                new SearchTableDto(5L, "T05", 4, false, 92, List.of("Fits party size well")),
                List.of(new SearchTableDto(6L, "T06", 4, false, 81, List.of("Located in selected zone")))
        );
        when(searchService.searchTables(any(SearchTablesRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-03-17",
                                  "time": "19:00",
                                  "partySize": 4,
                                  "zone": "MAIN",
                                  "windowSide": true,
                                  "quietArea": false,
                                  "accessible": true,
                                  "nearPlayArea": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedTable.id").value(5))
                .andExpect(jsonPath("$.recommendedTable.score").value(92))
                .andExpect(jsonPath("$.alternativeTables[0].id").value(6));
    }

    @Test
    void returnsBadRequestWhenDateIsMissing() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "time": "19:00",
                                  "partySize": 4
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestWhenPartySizeIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-03-17",
                                  "time": "19:00",
                                  "partySize": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void supportsQuiteAreaJsonAliasInRequestBody() throws Exception {
        when(searchService.searchTables(any(SearchTablesRequestDto.class)))
                .thenReturn(new SearchResultDto(null, List.of()));

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-03-17",
                                  "time": "19:00",
                                  "partySize": 2,
                                  "quiteArea": true
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<SearchTablesRequestDto> captor =
                ArgumentCaptor.forClass(SearchTablesRequestDto.class);
        verify(searchService).searchTables(captor.capture());
        assertThat(captor.getValue().quietArea()).isTrue();
    }
}

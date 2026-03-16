package com.example.restaurant.search;

import com.example.restaurant.dto.SearchResultDto;
import com.example.restaurant.dto.SearchTablesRequestDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public SearchResultDto searchTables(@Valid @RequestBody SearchTablesRequestDto request) {
        return searchService.searchTables(request);
    }
}

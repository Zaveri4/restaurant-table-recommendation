package com.example.restaurant;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {

    @GetMapping("/api/health")
    Map<String, Object> health() {
        return Map.of("status", "ok", "time", Instant.now().toString());
    }
}

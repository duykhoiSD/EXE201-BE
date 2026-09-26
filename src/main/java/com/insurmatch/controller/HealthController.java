package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check — GET /api/health
 * Khớp với FE: checkBackendHealth()
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "online");
        result.put("service", "insurmatch-api");

        try (Connection conn = dataSource.getConnection()) {
            result.put("database", conn.isValid(2) ? "connected" : "disconnected");
        } catch (Exception e) {
            result.put("database", "disconnected");
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}

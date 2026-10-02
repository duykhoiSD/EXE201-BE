package com.insurmatch.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health & Root Status Check — GET /, GET /health, GET /api/health
 * Đáp ứng Render Health Check và kiểm tra trạng thái API trực tiếp từ trình duyệt
 */
@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping({"/", "/health", "/api/health"})
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "online");
        result.put("service", "insurmatch-api");
        result.put("version", "1.0.0");
        result.put("message", "InsurMatch API is running smoothly");
        result.put("timestamp", LocalDateTime.now().toString());

        try (Connection conn = dataSource.getConnection()) {
            result.put("database", conn.isValid(2) ? "connected" : "disconnected");
        } catch (Exception e) {
            result.put("database", "disconnected");
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}


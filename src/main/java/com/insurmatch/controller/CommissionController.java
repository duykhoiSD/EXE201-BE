package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.Commission;
import com.insurmatch.service.CommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CommissionController — Khớp với FE:
 *   GET    /api/commissions?agentName=&period=&status=&carrier=
 *   GET    /api/commissions/summary?agentName=
 *   POST   /api/commissions
 *   PUT    /api/commissions/:id
 *   POST   /api/commissions/calculate
 */
@RestController
@RequestMapping("/api/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Commission>>> getCommissions(
            @RequestParam(required = false) String agentName,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String carrier) {
        List<Commission> commissions = commissionService.getAllCommissions(agentName, period, status, carrier);
        return ResponseEntity.ok(ApiResponse.success(commissions));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @RequestParam(required = false) String agentName) {
        Map<String, Object> summary = commissionService.getSummary(agentName);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Commission>> createCommission(@RequestBody Commission commission) {
        Commission created = commissionService.createCommission(commission);
        return ResponseEntity.ok(ApiResponse.success("Commission created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Commission>> updateCommission(@PathVariable Long id, @RequestBody Commission data) {
        Commission updated = commissionService.updateCommission(id, data);
        return ResponseEntity.ok(ApiResponse.success("Commission updated", updated));
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateCommissions() {
        Map<String, Object> result = commissionService.calculateCommissions();
        return ResponseEntity.ok(ApiResponse.success("Calculation complete", result));
    }
}

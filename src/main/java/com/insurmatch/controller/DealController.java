package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.Deal;
import com.insurmatch.service.DealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DealController — Khớp với FE:
 *   GET    /api/deals?search=&stage=&pipeline=
 *   GET    /api/deals/:id
 *   PUT    /api/deals/:id
 *   PUT    /api/deals/:id/admin
 */
@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Deal>>> getDeals(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String pipeline) {
        List<Deal> deals = dealService.getAllDeals(search, stage, pipeline);
        return ResponseEntity.ok(ApiResponse.success(deals));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Deal>> getDeal(@PathVariable Long id) {
        Deal deal = dealService.getDealById(id);
        return ResponseEntity.ok(ApiResponse.success(deal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Deal>> updateDeal(@PathVariable Long id, @RequestBody Deal dealData) {
        Deal updated = dealService.updateDeal(id, dealData);
        return ResponseEntity.ok(ApiResponse.success("Deal updated", updated));
    }

    @PutMapping("/{id}/admin")
    public ResponseEntity<ApiResponse<Deal>> updateDealAdmin(@PathVariable Long id, @RequestBody Deal adminData) {
        Deal updated = dealService.updateDealAdmin(id, adminData);
        return ResponseEntity.ok(ApiResponse.success("Deal admin fields updated", updated));
    }
}

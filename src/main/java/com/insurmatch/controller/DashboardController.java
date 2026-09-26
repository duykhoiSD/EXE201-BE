package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.service.ContactService;
import com.insurmatch.service.DealService;
import com.insurmatch.service.QuoteRequestService;
import com.insurmatch.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * DashboardController — Khớp với FE:
 *   GET /api/dashboard/stats
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ContactService contactService;
    private final DealService dealService;
    private final TicketService ticketService;
    private final QuoteRequestService quoteRequestService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalContacts", contactService.count());
        stats.put("totalDeals", dealService.count());
        stats.put("totalTickets", ticketService.count());
        stats.put("totalQuotes", quoteRequestService.count());
        stats.put("newQuotes", quoteRequestService.countByStatus("NEW"));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

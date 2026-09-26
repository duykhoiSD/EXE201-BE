package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.QuoteRequest;
import com.insurmatch.service.QuoteRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * QuoteController — Khớp với FE QuotePage / QuoteModal:
 *   POST /api/quotes   (public submit from /get-quote page)
 */
@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteRequestService quoteRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<QuoteRequest>> submitQuote(@RequestBody QuoteRequest quote) {
        QuoteRequest created = quoteRequestService.createQuote(quote);
        return ResponseEntity.ok(ApiResponse.success("Quote request submitted", created));
    }
}

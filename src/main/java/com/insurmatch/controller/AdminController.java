package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.AuditLog;
import com.insurmatch.entity.QuoteRequest;
import com.insurmatch.entity.User;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.AuditLogRepository;
import com.insurmatch.repository.UserRepository;
import com.insurmatch.service.ContactService;
import com.insurmatch.service.DealService;
import com.insurmatch.service.QuoteRequestService;
import com.insurmatch.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminController — Khớp với FE:
 *   GET    /api/admin/stats
 *   GET    /api/admin/accounts
 *   POST   /api/admin/accounts
 *   PUT    /api/admin/accounts/:id
 *   GET    /api/admin/quotes
 *   PUT    /api/admin/quotes/:id/assign
 *   GET    /api/admin/audit-logs
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuoteRequestService quoteRequestService;
    private final ContactService contactService;
    private final DealService dealService;
    private final TicketService ticketService;
    private final AuditLogRepository auditLogRepository;

    // ── Admin Stats ──────────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalContacts", contactService.count());
        stats.put("totalDeals", dealService.count());
        stats.put("totalTickets", ticketService.count());
        stats.put("totalQuotes", quoteRequestService.count());
        stats.put("newQuotes", quoteRequestService.countByStatus("NEW"));
        stats.put("assignedQuotes", quoteRequestService.countByStatus("ASSIGNED"));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ── Account Management ───────────────────────────────────────────────────
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<User>>> getAccounts() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<User>> createAccount(@RequestBody User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email already exists"));
        }
        // Hash password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User created = userRepository.save(user);

        // Audit log
        auditLogRepository.save(AuditLog.builder()
                .action("CREATE")
                .entityType("User")
                .entityId(created.getId())
                .details("Created account: " + created.getEmail() + " with role " + created.getRole())
                .performedBy("admin")
                .build());

        return ResponseEntity.ok(ApiResponse.success("Account created", created));
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<User>> updateAccount(@PathVariable Long id, @RequestBody User userData) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (userData.getFirstName() != null) existing.setFirstName(userData.getFirstName());
        if (userData.getLastName() != null) existing.setLastName(userData.getLastName());
        if (userData.getPhone() != null) existing.setPhone(userData.getPhone());
        if (userData.getRole() != null) existing.setRole(userData.getRole());
        if (userData.getActive() != null) existing.setActive(userData.getActive());
        if (userData.getNpn() != null) existing.setNpn(userData.getNpn());
        if (userData.getPassword() != null && !userData.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(userData.getPassword()));
        }

        User updated = userRepository.save(existing);

        // Audit log
        auditLogRepository.save(AuditLog.builder()
                .action("UPDATE")
                .entityType("User")
                .entityId(updated.getId())
                .details("Updated account: " + updated.getEmail())
                .performedBy("admin")
                .build());

        return ResponseEntity.ok(ApiResponse.success("Account updated", updated));
    }

    // ── Quote Management ─────────────────────────────────────────────────────
    @GetMapping("/quotes")
    public ResponseEntity<ApiResponse<List<QuoteRequest>>> getQuotes() {
        List<QuoteRequest> quotes = quoteRequestService.getAllQuotes();
        return ResponseEntity.ok(ApiResponse.success(quotes));
    }

    @PutMapping("/quotes/{id}/assign")
    public ResponseEntity<ApiResponse<QuoteRequest>> assignQuote(
            @PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long agentId = body.get("agentId");
        if (agentId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("agentId is required"));
        }
        QuoteRequest assigned = quoteRequestService.assignAgent(id, agentId);

        // Audit log
        auditLogRepository.save(AuditLog.builder()
                .action("ASSIGN")
                .entityType("QuoteRequest")
                .entityId(id)
                .details("Assigned quote #" + id + " to agent #" + agentId)
                .performedBy("admin")
                .build());

        return ResponseEntity.ok(ApiResponse.success("Quote assigned to agent", assigned));
    }

    // ── Audit Logs ───────────────────────────────────────────────────────────
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}

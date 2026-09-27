package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.dto.admin.AccountResponse;
import com.insurmatch.dto.admin.CreateAccountRequest;
import com.insurmatch.entity.AuditLog;
import com.insurmatch.entity.QuoteRequest;
import com.insurmatch.entity.User;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.AuditLogRepository;
import com.insurmatch.repository.UserRepository;
import com.insurmatch.service.ContactService;
import com.insurmatch.service.DealService;
import com.insurmatch.service.EmailService;
import com.insurmatch.service.QuoteRequestService;
import com.insurmatch.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Collection;
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
    private final EmailService emailService;
    private final QuoteRequestService quoteRequestService;
    private final ContactService contactService;
    private final DealService dealService;
    private final TicketService ticketService;
    private final AuditLogRepository auditLogRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

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
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccounts() {
        List<AccountResponse> accounts = userRepository.findAll().stream()
                .map(AccountResponse::fromUser)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    /**
     * Admin tạo tài khoản Agent / Staff mới (Khớp form AdminAccountsTab.jsx):
     * - Tự động tách họ tên
     * - Tự sinh mật khẩu khởi tạo an toàn
     * - Lưu User vào Database
     * - Gửi email bàn giao tài khoản về Gmail cho thành viên mới
     */
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email đã tồn tại trong hệ thống"));
        }

        // Tách họ và tên từ trường 'name' của form FE
        String rawName = request.getName().trim();
        String firstName = rawName;
        String lastName = "";
        int spaceIdx = rawName.lastIndexOf(' ');
        if (spaceIdx > 0) {
            firstName = rawName.substring(0, spaceIdx).trim();
            lastName = rawName.substring(spaceIdx + 1).trim();
        }

        // Parse danh sách bang cấp phép
        String statesStr = "";
        if (request.getStatesLicensed() instanceof Collection<?> col) {
            statesStr = String.join(", ", col.stream().map(Object::toString).toList());
        } else if (request.getStatesLicensed() != null) {
            statesStr = request.getStatesLicensed().toString();
        }

        // Xác định Role
        User.Role role = User.Role.AGENT;
        String reqRole = request.getRole() != null ? request.getRole().trim().toLowerCase() : "agent";
        if ("staff".equals(reqRole)) {
            role = User.Role.STAFF;
        } else if ("admin".equals(reqRole)) {
            role = User.Role.ADMIN;
        }

        // Sinh mật khẩu khởi tạo ngẫu nhiên nhưng dễ nhớ: InsurMatch@XXXX
        String tempPassword = "InsurMatch@" + (1000 + RANDOM.nextInt(9000));

        // Trạng thái ban đầu: Agent cần duyệt NPN -> Pending NPN; Staff/Admin -> Active
        String initialStatus = (role == User.Role.AGENT) ? "Pending NPN" : "Active";
        String complianceStatus = (role == User.Role.AGENT) ? "Pending Accreditation" : "Verified & Cleared";

        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(tempPassword))
                .firstName(firstName)
                .lastName(lastName)
                .phone(request.getPhone())
                .role(role)
                .active(true)
                .emailVerified(true)
                .npn(request.getNpn())
                .department(request.getDepartment() != null ? request.getDepartment() : "Regional Agent Network")
                .statesLicensed(statesStr)
                .status(initialStatus)
                .complianceStatus(complianceStatus)
                .dealsCount(0)
                .build();

        User created = userRepository.save(user);

        // Gửi email chào mừng kèm thông tin đăng nhập về Gmail của thành viên mới
        emailService.sendWelcomeAccountEmail(created.getEmail(), rawName, tempPassword, role.name());

        // Audit log
        auditLogRepository.save(AuditLog.builder()
                .action("CREATE")
                .entityType("User")
                .entityId(created.getId())
                .details("Admin provisioned account: " + created.getEmail() + " with role " + created.getRole())
                .performedBy("admin")
                .build());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khởi tạo tài khoản thành viên thành công!", AccountResponse.fromUser(created)));
    }

    /**
     * Admin cập nhật thông tin / phê duyệt / đình chỉ tài khoản:
     */
    @PutMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với id: " + id));

        if (updates.containsKey("status") && updates.get("status") != null) {
            existing.setStatus(updates.get("status").toString());
            // Nếu đình chỉ thì active = false, ngược lại true
            if ("Suspended".equalsIgnoreCase(updates.get("status").toString())) {
                existing.setActive(false);
            } else if ("Active".equalsIgnoreCase(updates.get("status").toString())) {
                existing.setActive(true);
            }
        }

        if (updates.containsKey("complianceStatus") && updates.get("complianceStatus") != null) {
            existing.setComplianceStatus(updates.get("complianceStatus").toString());
        }

        if (updates.containsKey("suspensionReason")) {
            existing.setSuspensionReason(updates.get("suspensionReason") != null ? updates.get("suspensionReason").toString() : "");
        }

        if (updates.containsKey("phone") && updates.get("phone") != null) {
            existing.setPhone(updates.get("phone").toString());
        }

        if (updates.containsKey("npn") && updates.get("npn") != null) {
            existing.setNpn(updates.get("npn").toString());
        }

        if (updates.containsKey("department") && updates.get("department") != null) {
            existing.setDepartment(updates.get("department").toString());
        }

        if (updates.containsKey("statesLicensed") && updates.get("statesLicensed") != null) {
            Object sl = updates.get("statesLicensed");
            if (sl instanceof Collection<?> col) {
                existing.setStatesLicensed(String.join(", ", col.stream().map(Object::toString).toList()));
            } else {
                existing.setStatesLicensed(sl.toString());
            }
        }

        if (updates.containsKey("role") && updates.get("role") != null) {
            String roleStr = updates.get("role").toString().toUpperCase();
            try {
                existing.setRole(User.Role.valueOf(roleStr));
            } catch (Exception ignored) {}
        }

        User updated = userRepository.save(existing);

        // Audit log
        auditLogRepository.save(AuditLog.builder()
                .action("UPDATE")
                .entityType("User")
                .entityId(updated.getId())
                .details("Admin updated account: " + updated.getEmail() + " | Status: " + updated.getStatus())
                .performedBy("admin")
                .build());

        return ResponseEntity.ok(ApiResponse.success("Cập nhật tài khoản thành công!", AccountResponse.fromUser(updated)));
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

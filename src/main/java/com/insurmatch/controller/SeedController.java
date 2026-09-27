package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.*;
import com.insurmatch.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * SeedController — Khớp với FE:
 *   POST /api/seed
 *
 * Reset database và nạp dữ liệu mẫu để phục vụ demo/test.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeedController {

    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final DealRepository dealRepository;
    private final TicketRepository ticketRepository;
    private final TaskRepository taskRepository;
    private final QuoteRequestRepository quoteRequestRepository;
    private final NoteRepository noteRepository;
    private final ActivityRepository activityRepository;
    private final CommissionRepository commissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedDatabase() {
        // Clear all data in correct order (respect FK constraints)
        commissionRepository.deleteAll();
        activityRepository.deleteAll();
        noteRepository.deleteAll();
        taskRepository.deleteAll();
        ticketRepository.deleteAll();
        dealRepository.deleteAll();
        quoteRequestRepository.deleteAll();
        contactRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        // ── Seed Users ───────────────────────────────────────────────────────
        User admin = userRepository.save(User.builder()
                .email("admin@insurmatch.us")
                .password(passwordEncoder.encode("Admin@123"))
                .firstName("Super").lastName("Admin")
                .role(User.Role.ADMIN).active(true).emailVerified(true).build());

        User staff = userRepository.save(User.builder()
                .email("staff@insurmatch.us")
                .password(passwordEncoder.encode("Staff@123"))
                .firstName("Platform").lastName("Staff")
                .phone("832-555-0101")
                .role(User.Role.SUPPORT).active(true).emailVerified(true).build());

        User agent = userRepository.save(User.builder()
                .email("agent@insurmatch.us")
                .password(passwordEncoder.encode("Agent@123"))
                .firstName("Licensed").lastName("Agent")
                .phone("832-555-0202")
                .npn("20011862")
                .role(User.Role.AGENT).active(true).emailVerified(true).build());

        User manager = userRepository.save(User.builder()
                .email("manager@insurmatch.us")
                .password(passwordEncoder.encode("Manager@123"))
                .firstName("Khanh").lastName("Nguyen")
                .phone("832-555-0303")
                .role(User.Role.MANAGER).active(true).emailVerified(true).build());

        // ── Seed Contacts ────────────────────────────────────────────────────
        Contact c1 = contactRepository.save(Contact.builder()
                .firstName("Minh").lastName("Tran")
                .email("minhtran@gmail.com").phone("832-123-4567")
                .dateOfBirth(LocalDate.of(1985, 3, 15))
                .gender("Male").immigrationStatus("Permanent Resident")
                .address("1234 Bellaire Blvd").city("Houston").state("TX").zipCode("77072")
                .householdSize(4).estimatedIncome(45000.0)
                .sourceChannel("Referral").sourceDetail("Agent Khanh")
                .contactOwner(manager).supportAgent(staff)
                .build());

        Contact c2 = contactRepository.save(Contact.builder()
                .firstName("Lan").lastName("Nguyen")
                .email("lannguyen@gmail.com").phone("714-987-6543")
                .dateOfBirth(LocalDate.of(1960, 7, 22))
                .gender("Female").immigrationStatus("US Citizen")
                .address("9876 Bolsa Ave").city("Westminster").state("CA").zipCode("92683")
                .householdSize(2).estimatedIncome(28000.0)
                .sourceChannel("TeleSales").sourceDetail("OB Team")
                .contactOwner(agent).supportAgent(staff)
                .build());

        // ── Seed Deals ───────────────────────────────────────────────────────
        Deal d1 = dealRepository.save(Deal.builder()
                .dealName("Minh Tran — Ambetter 2026")
                .pipeline("OBAMACARE").dealStage("ENROLLED_ACTIVE")
                .carrier("Ambetter").planName("Ambetter Balanced Care 3")
                .amount(BigDecimal.valueOf(125.50))
                .policyEffectiveDate(LocalDate.of(2026, 1, 1))
                .policyId("POL-78234").memberId("MID-98234710")
                .householdSize(4).applicantCount(3)
                .estimatedIncome(BigDecimal.valueOf(45000))
                .enrolledNpn("20011862")
                .brokerEffectiveDate(LocalDate.of(2025, 12, 15))
                .paymentStatus("AUTOPAY")
                .payThroughDate(LocalDate.of(2026, 9, 30))
                .chooseDoctorStatus("DONE").doctorName("Dr. Pham Van An")
                .contact(c1).dealOwner(agent).supportAgent(staff)
                .build());

        Deal d2 = dealRepository.save(Deal.builder()
                .dealName("Lan Nguyen — Medicare Advantage 2026")
                .pipeline("MEDICARE").dealStage("ENROLLED")
                .carrier("UHC").planName("UHC Dual Complete HMO")
                .amount(BigDecimal.valueOf(0))
                .policyEffectiveDate(LocalDate.of(2026, 1, 1))
                .contact(c2).dealOwner(agent).supportAgent(staff)
                .build());

        // ── Seed Tickets ─────────────────────────────────────────────────────
        ticketRepository.save(Ticket.builder()
                .ticketName("Make September Payment — Minh Tran")
                .pipeline("PAYMENT")
                .ticketStatus("CHECK_PAYMENT")
                .ticketDescription("Monthly payment for September 2026")
                .priority("HIGH")
                .dueDate(LocalDate.of(2026, 9, 30))
                .contact(c1).deal(d1).ticketOwner(agent).serviceAgent(staff)
                .build());

        ticketRepository.save(Ticket.builder()
                .ticketName("Upload Consent Form — Lan Nguyen")
                .pipeline("COLLECT_DOCUMENT")
                .ticketStatus("CONTACT_CLIENT_FOR_UPLOAD")
                .ticketDescription("Collect consent form within 30 days of enrollment")
                .priority("MEDIUM")
                .dueDate(LocalDate.of(2026, 10, 15))
                .contact(c2).deal(d2).ticketOwner(agent).serviceAgent(staff)
                .build());

        // ── Seed Tasks ───────────────────────────────────────────────────────
        taskRepository.save(Task.builder()
                .title("Follow up 1st Payment — Minh Tran")
                .description("Confirm first payment cleared for Ambetter plan")
                .priority("HIGH").status("IN_PROGRESS")
                .dueDate(LocalDate.of(2026, 9, 28))
                .contact(c1).deal(d1).assignedTo(staff).createdBy(manager)
                .build());

        taskRepository.save(Task.builder()
                .title("Send PCP list — Lan Nguyen")
                .description("Find Vietnamese-speaking PCP near Westminster CA")
                .priority("MEDIUM").status("OPEN")
                .dueDate(LocalDate.now().plusDays(3))
                .contact(c2).deal(d2).assignedTo(staff).createdBy(manager)
                .build());

        // ── Seed QuoteRequests ───────────────────────────────────────────────
        quoteRequestRepository.save(QuoteRequest.builder()
                .fullName("Hoa Pham").email("hoapham@gmail.com").phone("281-555-1234")
                .zipCode("77036").state("TX")
                .insuranceType("OBAMACARE").householdSize(3).estimatedIncome(38000.0)
                .status("NEW").build());

        quoteRequestRepository.save(QuoteRequest.builder()
                .fullName("Duc Le").email("ducle@gmail.com").phone("408-555-9876")
                .zipCode("95122").state("CA")
                .insuranceType("MEDICARE").householdSize(1).estimatedIncome(20000.0)
                .status("NEW").build());

        // ── Seed Notes ───────────────────────────────────────────────────────
        noteRepository.save(Note.builder()
                .text("Enrolled / Changed NPN — Ambetter Balanced Care 3 — $125.50\nPlan Effective 01/01/2026 — HH4A3 — Income $45,000\nNeed: 1st payment + Choose Dr + No upload")
                .author("Khanh Nguyen").date("Sep 18, 2026").time("10:30 AM")
                .contact(c1).deal(d1).build());

        // ── Seed Commissions ─────────────────────────────────────────────────
        commissionRepository.save(Commission.builder()
                .agentName("Licensed Agent").agentNpn("20011862")
                .carrier("Ambetter").planName("Ambetter Balanced Care 3")
                .policyId("POL-78234").memberCount(3)
                .grossAmount(75.0).supportDeduction(15.0).netAmount(60.0)
                .commissionType("ACA_PMPM").saleSupportStatus("NONE")
                .period("2026-09").status("PENDING")
                .deal(d1).build());

        Map<String, Object> result = Map.of(
                "users", 4,
                "contacts", 2,
                "deals", 2,
                "tickets", 2,
                "tasks", 2,
                "quoteRequests", 2,
                "notes", 1,
                "commissions", 1
        );

        return ResponseEntity.ok(ApiResponse.success("Database seeded successfully", result));
    }
}

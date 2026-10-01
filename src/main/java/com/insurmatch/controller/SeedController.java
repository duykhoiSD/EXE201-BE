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
import org.springframework.jdbc.core.JdbcTemplate;

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
    private final TicketCommentRepository ticketCommentRepository;
    private final TaskRepository taskRepository;
    private final QuoteRequestRepository quoteRequestRepository;
    private final NoteRepository noteRepository;
    private final ActivityRepository activityRepository;
    private final CommissionRepository commissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final CustomerDocumentRepository documentRepository;
    private final DocumentFileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedDatabase() {
        try {
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;");
        } catch (Exception e) {
            // Ignore if constraint doesn't exist or table doesn't exist
        }

        // Clear all data in correct order (respect FK constraints)
        fileRepository.deleteAll();
        documentRepository.deleteAll();
        commissionRepository.deleteAll();
        activityRepository.deleteAll();
        noteRepository.deleteAll();
        taskRepository.deleteAll();
        ticketCommentRepository.deleteAll();
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
                .department("Executive Office")
                .status("Active")
                .complianceStatus("Verified & Cleared")
                .role(User.Role.ADMIN).active(true).emailVerified(true).build());

        User staff = userRepository.save(User.builder()
                .email("staff@insurmatch.us")
                .password(passwordEncoder.encode("Staff@123"))
                .firstName("Platform").lastName("Staff")
                .phone("832-555-0101")
                .department("Operations Hub")
                .status("Active")
                .complianceStatus("Verified & Cleared")
                .role(User.Role.STAFF).active(true).emailVerified(true).build());

        User agent = userRepository.save(User.builder()
                .email("agent@insurmatch.us")
                .password(passwordEncoder.encode("Agent@123"))
                .firstName("Licensed").lastName("Agent Partner")
                .phone("832-555-0202")
                .npn("20011862")
                .department("Regional Agent Network")
                .statesLicensed("TX (TDI), CA (CDI)")
                .status("Active")
                .complianceStatus("Verified & Cleared")
                .dealsCount(5)
                .role(User.Role.AGENT).active(true).emailVerified(true).build());

        User manager = userRepository.save(User.builder()
                .email("manager@insurmatch.us")
                .password(passwordEncoder.encode("Manager@123"))
                .firstName("Khanh").lastName("Nguyen")
                .phone("832-555-0303")
                .department("Regional Management")
                .status("Active")
                .complianceStatus("Verified & Cleared")
                .role(User.Role.MANAGER).active(true).emailVerified(true).build());

        // ── Seed Contacts ────────────────────────────────────────────────────
        Contact c1 = contactRepository.save(Contact.builder()
                .code("CT26002600")
                .firstName("Nhat Huu Tuan").lastName("Dang")
                .email("tuannhat.n2@gmail.com").phone("714-837-2395")
                .dateOfBirth(LocalDate.of(1985, 3, 15))
                .gender("Male").immigrationStatus("Permanent Resident")
                .address("1234 Bellaire Blvd").city("Houston").state("TX").zipCode("77072")
                .householdSize(4).estimatedIncome(45000.0)
                .sourceChannel("Referral").sourceDetail("The Best Rate Insurance")
                .contactOwner(manager).supportAgent(staff)
                .build());

        Contact c2 = contactRepository.save(Contact.builder()
                .code("CT26002601")
                .firstName("Minh").lastName("Tran")
                .email("minhtran@gmail.com").phone("832-123-4567")
                .dateOfBirth(LocalDate.of(1985, 3, 15))
                .gender("Male").immigrationStatus("Permanent Resident")
                .address("1234 Bellaire Blvd").city("Houston").state("TX").zipCode("77072")
                .householdSize(4).estimatedIncome(45000.0)
                .sourceChannel("Referral").sourceDetail("Agent Khanh")
                .contactOwner(manager).supportAgent(staff)
                .build());

        Contact c3 = contactRepository.save(Contact.builder()
                .code("CT26002602")
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
                .code("D26005033")
                .dealName("Non-CMS - Minh Tran - OB 10/2026 (NC)")
                .pipeline("Obamacare 2026")
                .dealStage("Ready to Enroll (Obamacare 2026)")
                .carrier("BCBS")
                .planName("Blue Advantage Silver POS")
                .amount(BigDecimal.valueOf(125.50))
                .closeDate("10/15/2026")
                .sellingState("North Carolina (NC)")
                .member("Minh Tran")
                .primaryMemberId("MID-98234710")
                .numberMember(3)
                .householdSize(4)
                .applicantCount(3)
                .estimatedIncome(BigDecimal.valueOf(45000))
                .enrolledAddress("4301 Laurel Pond Way, Raleigh, NC 27616")
                .quotedCounty("Wake County")
                .isBackdateDeal("No")
                .enrolledNpn("20011862")
                .brokerEffectiveDate(LocalDate.of(2025, 12, 15))
                .saleSupportStatus("None")
                .monthlyPremium(BigDecimal.valueOf(450.00))
                .subsidyAmount(BigDecimal.valueOf(324.50))
                .agencyCommission(BigDecimal.valueOf(25.00))
                .bonusTier("Standard Tier")
                .paymentStatus("AUTOPAY")
                .payThroughDate(LocalDate.of(2026, 9, 30))
                .chooseDoctorStatus("DONE")
                .doctorName("Dr. Pham Van An")
                .contact(c1)
                .dealOwner(agent)
                .supportAgent(staff)
                .build());

        Deal d2 = dealRepository.save(Deal.builder()
                .code("D26005034")
                .dealName("Non-CMS - Lan Nguyen - MC 2026 (CA)")
                .pipeline("Medicare 2026")
                .dealStage("Ready to Enroll (Medicare 2026)")
                .carrier("UHC")
                .planName("UHC Dual Complete HMO")
                .amount(BigDecimal.valueOf(0))
                .closeDate("11/01/2026")
                .sellingState("California (CA)")
                .member("Lan Nguyen")
                .primaryMemberId("MID-38291039")
                .numberMember(1)
                .householdSize(2)
                .applicantCount(1)
                .estimatedIncome(BigDecimal.valueOf(28000))
                .enrolledAddress("9876 Bolsa Ave, Westminster, CA 92683")
                .quotedCounty("Orange County")
                .isBackdateDeal("No")
                .enrolledNpn("20011862")
                .brokerEffectiveDate(LocalDate.of(2026, 1, 1))
                .saleSupportStatus("Partial")
                .contact(c3)
                .dealOwner(manager)
                .supportAgent(staff)
                .build());

        // ── Seed Tickets ─────────────────────────────────────────────────────
        ticketRepository.save(Ticket.builder()
                .code("TC26001001")
                .ticketName("Upload documents - Nhat Huu Tuan Dang")
                .pipeline("Upload document")
                .ticketStatus("Waiting on verification")
                .ticketDescription("Monthly payment for September 2026")
                .priority("HIGH")
                .dueDate(LocalDate.of(2026, 9, 30))
                .contact(c1).deal(d1).ticketOwner(agent).serviceAgent(staff)
                .build());

        ticketRepository.save(Ticket.builder()
                .code("TC26001002")
                .ticketName("ACA account 2026")
                .pipeline("ACA account")
                .ticketStatus("DONE")
                .ticketDescription("Collect consent form within 30 days of enrollment")
                .priority("MEDIUM")
                .dueDate(LocalDate.of(2026, 10, 15))
                .contact(c3).deal(d2).ticketOwner(agent).serviceAgent(staff)
                .build());

        // ── Seed Tasks ───────────────────────────────────────────────────────
        taskRepository.save(Task.builder()
                .code("TSK26001001")
                .title("Follow up 1st Payment — Minh Tran")
                .description("Confirm first payment cleared for Ambetter plan")
                .priority("HIGH").status("IN_PROGRESS")
                .dueDate(LocalDate.of(2026, 9, 28))
                .contact(c1).deal(d1).assignedTo(staff).createdBy(manager)
                .build());

        taskRepository.save(Task.builder()
                .code("TSK26001002")
                .title("Send PCP list — Lan Nguyen")
                .description("Find Vietnamese-speaking PCP near Westminster CA")
                .priority("MEDIUM").status("OPEN")
                .dueDate(LocalDate.now().plusDays(3))
                .contact(c3).deal(d2).assignedTo(staff).createdBy(manager)
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

        // ── Seed Activities ──────────────────────────────────────────────────
        activityRepository.save(Activity.builder()
                .type("Deal Activity")
                .actor("Khanh Nguyen (khanhnguyen31@7)")
                .time("09/09/2026, 13:05")
                .summary("moved deal stage from \"Waiting for document (Obamacare 2026)\" to \"Ready to Enroll (Obamacare 2026)\"")
                .dealTitle(d1.getDealName())
                .linkText("View Details")
                .contact(c1).deal(d1).build());

        activityRepository.save(Activity.builder()
                .type("Deal Created")
                .actor("Platform Staff")
                .time("09/01/2026, 09:00")
                .summary("Created deal: " + d2.getDealName())
                .dealTitle(d2.getDealName())
                .contact(c3).deal(d2).build());

        // ── Seed Commissions ─────────────────────────────────────────────────
        commissionRepository.save(Commission.builder()
                .agentName("Licensed Agent").agentNpn("20011862")
                .carrier("Ambetter").planName("Ambetter Balanced Care 3")
                .policyId("POL-78234").memberCount(3)
                .grossAmount(75.0).supportDeduction(15.0).netAmount(60.0)
                .commissionType("ACA_PMPM").saleSupportStatus("NONE")
                .period("2026-09").status("PENDING")
                .deal(d1).build());

        // ── Seed Customer Documents & Files ───────────────────────────────────
        CustomerDocument doc1 = documentRepository.save(CustomerDocument.builder()
                .code("DOC-01")
                .name("Hai Nguyen")
                .initials("HN")
                .contactOwner("Khanh Nguyen (khanhnguyen31@7)")
                .lastModifiedBy("Khanh Nguyen")
                .contact(c1)
                .build());

        fileRepository.save(DocumentFile.builder()
                .document(doc1)
                .category("identity")
                .name("Driver_License_Front.jpg")
                .fullName("Driver_License_Front.jpg")
                .size("2.1 MB")
                .type("image")
                .build());

        fileRepository.save(DocumentFile.builder()
                .document(doc1)
                .category("consentFormMkp")
                .name("CMS_Marketplace_Consent.pdf")
                .fullName("CMS_Marketplace_Consent.pdf")
                .size("480 KB")
                .type("pdf")
                .build());

        fileRepository.save(DocumentFile.builder()
                .document(doc1)
                .category("paymentInformation")
                .name("Bank_Debit_Auth_Form.pdf")
                .fullName("Bank_Debit_Auth_Form.pdf")
                .size("320 KB")
                .type("pdf")
                .build());

        CustomerDocument doc2 = documentRepository.save(CustomerDocument.builder()
                .code("DOC-02")
                .name("Lan Nguyen")
                .initials("LN")
                .contactOwner("Licensed Agent (agent@insurmatch.us)")
                .lastModifiedBy("Licensed Agent")
                .contact(c3)
                .build());

        fileRepository.save(DocumentFile.builder()
                .document(doc2)
                .category("identity")
                .name("US_Passport_Scan.pdf")
                .fullName("US_Passport_Scan.pdf")
                .size("1.4 MB")
                .type("pdf")
                .build());

        fileRepository.save(DocumentFile.builder()
                .document(doc2)
                .category("consentFormText")
                .name("SMS_Confirmation_Screenshot.png")
                .fullName("SMS_Confirmation_Screenshot.png")
                .size("850 KB")
                .type("image")
                .build());

        fileRepository.save(DocumentFile.builder()
                .document(doc2)
                .category("tax")
                .name("Tax_1040_Income_Proof.pdf")
                .fullName("Tax_1040_Income_Proof.pdf")
                .size("2.3 MB")
                .type("pdf")
                .build());

        Map<String, Object> result = Map.of(
                "users", 4,
                "contacts", 2,
                "deals", 2,
                "tickets", 2,
                "tasks", 2,
                "quoteRequests", 2,
                "notes", 1,
                "commissions", 1,
                "documents", 2
        );

        return ResponseEntity.ok(ApiResponse.success("Database seeded successfully", result));
    }
}

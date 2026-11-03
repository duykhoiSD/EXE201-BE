package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.*;
import com.insurmatch.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * SeedController — POST /api/seed
 *
 * Khởi tạo tài khoản Demo thực chiến kèm 8 hồ sơ khách hàng mẫu 360°:
 * - 3 khách hàng sắp bước sang tuổi 65 (Turning 65 -> Chuyển từ ACA qua Medicare)
 * - 3 khách hàng sắp đến hạn gia hạn bảo hiểm ACA (ACA Renewal Approaching)
 * - 2 khách hàng đã hoàn tất gia hạn mẫu (ACA Active & Medicare AEP Active)
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
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedDatabase() {
        try {
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to drop constraint: " + e.getMessage()));
        }

        // Clear all data in correct FK order
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
        paymentTransactionRepository.deleteAll();
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();

        LocalDate today = LocalDate.now();

        // ── 1. Seed Users (Demo Accounts) ────────────────────────────────────
        userRepository.save(User.builder()
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

        // Tài khoản Agent Demo chính (Khanh Nguyen — khớp 100% với RBAC FE & BE)
        User demoAgent = userRepository.save(User.builder()
                .email("agent@insurmatch.us")
                .password(passwordEncoder.encode("Agent@123"))
                .firstName("Khanh").lastName("Nguyen")
                .phone("832-555-0202")
                .npn("20011862")
                .department("Independent Agency Demo")
                .statesLicensed("TX, CA, FL, NC, GA")
                .status("Active")
                .complianceStatus("Verified & Cleared")
                .dealsCount(8)
                .role(User.Role.AGENT).active(true).emailVerified(true).build());

        userRepository.save(User.builder()
                .email("manager@insurmatch.us")
                .password(passwordEncoder.encode("Manager@123"))
                .firstName("Sean").lastName("Ngo")
                .phone("832-555-0303")
                .npn("20088412")
                .department("Regional Management")
                .statesLicensed("TX, CA, FL")
                .status("Active")
                .complianceStatus("Verified & Cleared")
                .dealsCount(4)
                .role(User.Role.AGENT).active(true).emailVerified(true).build());

        // ── 2. Seed 8 Demo Contacts ──────────────────────────────────────────

        // [NHÓM 1: KHÁCH SẮP BƯỚC SANG TUỔI 65 -> CHUYỂN QUA MEDICARE]
        // Hồ sơ 1: Bà Trần Thị Mai — Còn 18 ngày tròn 65 tuổi
        Contact c1 = contactRepository.save(Contact.builder()
                .code("CT26002601")
                .firstName("Mai").middleName("Thi").lastName("Tran")
                .email("maitran61@gmail.com").phone("+1 714-839-2210")
                .dateOfBirth(today.minusYears(65).plusDays(18))
                .gender("Female").immigrationStatus("US Citizen").language("Vietnamese")
                .address("9211 Bolsa Ave").city("Westminster").county("Orange County").state("CA").zipCode("92683")
                .householdSize(2).estimatedIncome(26500.0)
                .acaStatus("VERIFIED").status("Active")
                .sourceChannel("Referral").sourceDetail("Turning 65 - Chuyển từ ACA sang Medicare")
                .contactOwner(demoAgent).supportAgent(staff)
                .build());

        // Hồ sơ 2: Ông Nguyễn Văn Hùng — Còn 42 ngày tròn 65 tuổi
        Contact c2 = contactRepository.save(Contact.builder()
                .code("CT26002602")
                .firstName("Hung").middleName("Van").lastName("Nguyen")
                .email("hungnguyen.tx@gmail.com").phone("+1 832-641-8890")
                .dateOfBirth(today.minusYears(65).plusDays(42))
                .gender("Male").immigrationStatus("US Citizen").language("Vietnamese")
                .address("11205 Bellaire Blvd").city("Houston").county("Harris County").state("TX").zipCode("77072")
                .householdSize(2).estimatedIncome(31000.0)
                .acaStatus("DONE").status("Active")
                .sourceChannel("Existing ACA Book").sourceDetail("Turning 65 IEP Window")
                .contactOwner(demoAgent).supportAgent(staff)
                .build());

        // Hồ sơ 3: Bà Lê Thu Hà — Còn 9 ngày tròn 65 tuổi (Khẩn cấp)
        Contact c3 = contactRepository.save(Contact.builder()
                .code("CT26002603")
                .firstName("Ha").middleName("Thu").lastName("Le")
                .email("lethuhaga@gmail.com").phone("+1 470-512-3304")
                .dateOfBirth(today.minusYears(65).plusDays(9))
                .gender("Female").immigrationStatus("Permanent Resident").language("Vietnamese")
                .address("5420 Jimmy Carter Blvd").city("Norcross").county("Gwinnett County").state("GA").zipCode("30093")
                .householdSize(1).estimatedIncome(22000.0)
                .acaStatus("DONE").status("Active")
                .sourceChannel("TeleSales").sourceDetail("Turning 65 Urgent Transition")
                .contactOwner(demoAgent).supportAgent(staff)
                .build());

        // [NHÓM 2: KHÁCH SẮP ĐẾN HẠN GIA HẠN BẢO HIỂM ACA (RENEWAL APPROACHING)]
        // Hồ sơ 4: Anh Đặng Nhật Hữu Tuấn — Hạn gia hạn ACA còn 5 ngày
        Contact c4 = contactRepository.save(Contact.builder()
                .code("CT26002604")
                .firstName("Nhat Huu Tuan").lastName("Dang")
                .email("tuannhat.n2@gmail.com").phone("+1 714-837-2395")
                .dateOfBirth(LocalDate.of(1985, 3, 15))
                .gender("Male").immigrationStatus("Permanent Resident").language("Vietnamese")
                .address("4301 Laurel Pond Way").city("Raleigh").county("Wake County").state("NC").zipCode("27616")
                .householdSize(4).estimatedIncome(45000.0)
                .acaStatus("PENDING").status("Active")
                .sourceChannel("Referral").sourceDetail("ACA Renewal - Cần xác minh Tax 1040")
                .contactOwner(demoAgent).supportAgent(staff)
                .build());

        // Hồ sơ 5: Chị Phạm Ngọc Lan — Hạn gia hạn ACA còn 7 ngày
        Contact c5 = contactRepository.save(Contact.builder()
                .code("CT26002605")
                .firstName("Ngoc Lan").lastName("Pham")
                .email("ngoclan.pham@gmail.com").phone("+1 972-488-7120")
                .dateOfBirth(LocalDate.of(1988, 8, 20))
                .gender("Female").immigrationStatus("US Citizen").language("Vietnamese")
                .address("3340 W Walnut St").city("Garland").county("Dallas County").state("TX").zipCode("75042")
                .householdSize(3).estimatedIncome(38500.0)
                .acaStatus("NEED_CALL_TO_VERIFIED").status("Active")
                .sourceChannel("Marketing").sourceDetail("ACA Open Enrollment Renewal")
                .contactOwner(demoAgent).supportAgent(staff)
                .build());

        // Hồ sơ 6: Anh Trần Minh Quân — Hạn gia hạn ACA còn 12 ngày
        Contact c6 = contactRepository.save(Contact.builder()
                .code("CT26002606")
                .firstName("Minh Quan").lastName("Tran")
                .email("minhquantran.fl@gmail.com").phone("+1 407-921-5540")
                .dateOfBirth(LocalDate.of(1981, 5, 10))
                .gender("Male").immigrationStatus("Permanent Resident").language("Vietnamese")
                .address("1820 E Colonial Dr").city("Orlando").county("Orange County").state("FL").zipCode("32803")
                .householdSize(4).estimatedIncome(52000.0)
                .acaStatus("VERIFIED").status("Active")
                .sourceChannel("Referral").sourceDetail("ACA Renewal & Đổi bác sĩ PCP")
                .contactOwner(demoAgent).supportAgent(staff)
                .build());

        // [NHÓM 3: HỒ SƠ ĐANG ACTIVE ỔN ĐỊNH ĐỂ SO SÁNH TRÊN PIPELINE]
        // Hồ sơ 7: Cô Vũ Phương Thảo — Đã gia hạn ACA thành công (Autopay)
        Contact c7 = contactRepository.save(Contact.builder()
                .code("CT26002607")
                .firstName("Phuong Thao").lastName("Vu")
                .email("phuongthaovu@gmail.com").phone("+1 281-773-9012")
                .dateOfBirth(LocalDate.of(1974, 11, 2))
                .gender("Female").immigrationStatus("US Citizen").language("Vietnamese")
                .address("11627 S Texas 6").city("Sugar Land").county("Fort Bend County").state("TX").zipCode("77498")
                .householdSize(2).estimatedIncome(34000.0)
                .acaStatus("DONE").status("Active")
                .sourceChannel("Referral").sourceDetail("Client trung thành 3 năm")
                .contactOwner(demoAgent).supportAgent(staff)
                .build());

        // Hồ sơ 8: Ông Hoàng Đức Trọng — Khách hàng Medicare hiện hữu (68 tuổi)
        Contact c8 = contactRepository.save(Contact.builder()
                .code("CT26002608")
                .firstName("Duc Trong").lastName("Hoang")
                .email("ductrong.sj@gmail.com").phone("+1 408-612-8839")
                .dateOfBirth(LocalDate.of(1958, 4, 18))
                .gender("Male").immigrationStatus("US Citizen").language("Vietnamese")
                .address("2150 Tully Rd").city("San Jose").county("Santa Clara County").state("CA").zipCode("95122")
                .householdSize(1).estimatedIncome(21000.0)
                .acaStatus("DONE").status("Active")
                .sourceChannel("Community Event").sourceDetail("Medicare Annual Review (AEP)")
                .contactOwner(demoAgent).supportAgent(staff)
                .build());

        // ── 3. Seed 8 Deals (Khớp 1-1 với 8 hồ sơ khách hàng) ────────────────
        Deal d1 = dealRepository.save(Deal.builder()
                .code("D26005031")
                .dealName("Turning 65 - Mai Thi Tran - Chuyển qua Medicare (CA)")
                .pipeline("Medicare 2026")
                .dealStage("Ready to Enroll (Medicare 2026)")
                .carrier("UHC")
                .planName("UHC Dual Complete Medicare HMO")
                .amount(BigDecimal.ZERO)
                .monthlyPremium(BigDecimal.ZERO)
                .agencyCommission(BigDecimal.valueOf(611.00))
                .policyId("MC-9920184")
                .primaryMemberId("MBI-4S92A81K")
                .member("Mai Thi Tran")
                .numberMember(1).householdSize(2).applicantCount(1)
                .estimatedIncome(BigDecimal.valueOf(26500))
                .sellingState("California (CA)")
                .enrolledAddress("9211 Bolsa Ave, Westminster, CA 92683")
                .quotedCounty("Orange County")
                .enrolledNpn("20011862")
                .policyEffectiveDate(today.plusDays(18).withDayOfMonth(1))
                .paymentStatus("ZERO_PLAN")
                .chooseDoctorStatus("DONE").doctorName("Dr. Tran Quoc Bao")
                .saleSupportStatus("NONE")
                .contact(c1).dealOwner(demoAgent).supportAgent(staff)
                .build());

        Deal d2 = dealRepository.save(Deal.builder()
                .code("D26005032")
                .dealName("Turning 65 - Hung Van Nguyen - Medicare IEP (TX)")
                .pipeline("Medicare 2026")
                .dealStage("New Opportunity/Call to Renew (Medicare 2026)")
                .carrier("Humana")
                .planName("Humana Gold Plus Medicare HMO")
                .amount(BigDecimal.ZERO)
                .monthlyPremium(BigDecimal.ZERO)
                .agencyCommission(BigDecimal.valueOf(611.00))
                .member("Hung Van Nguyen")
                .numberMember(1).householdSize(2).applicantCount(1)
                .estimatedIncome(BigDecimal.valueOf(31000))
                .sellingState("Texas (TX)")
                .enrolledAddress("11205 Bellaire Blvd, Houston, TX 77072")
                .quotedCounty("Harris County")
                .enrolledNpn("20011862")
                .uploadRequest(true).deadlineUpload(today.plusDays(14))
                .paymentStatus("ZERO_PLAN")
                .chooseDoctorStatus("NEED_CHOOSE_DOCTOR")
                .saleSupportStatus("PARTIAL")
                .contact(c2).dealOwner(demoAgent).supportAgent(staff)
                .build());

        Deal d3 = dealRepository.save(Deal.builder()
                .code("D26005033")
                .dealName("Turning 65 - Ha Thu Le - Medicare Advantage (GA)")
                .pipeline("Medicare 2026")
                .dealStage("Enrolled - Active (Medicare 2026)")
                .carrier("Aetna")
                .planName("Aetna Medicare Eagle PPO")
                .amount(BigDecimal.ZERO)
                .monthlyPremium(BigDecimal.ZERO)
                .agencyCommission(BigDecimal.valueOf(611.00))
                .policyId("MC-8841205")
                .primaryMemberId("MBI-7H11C94P")
                .member("Ha Thu Le")
                .numberMember(1).householdSize(1).applicantCount(1)
                .estimatedIncome(BigDecimal.valueOf(22000))
                .sellingState("Georgia (GA)")
                .enrolledAddress("5420 Jimmy Carter Blvd, Norcross, GA 30093")
                .quotedCounty("Gwinnett County")
                .enrolledNpn("20011862")
                .paymentStatus("ZERO_PLAN")
                .chooseDoctorStatus("DONE").doctorName("Dr. Le Minh Duc")
                .saleSupportStatus("NONE")
                .contact(c3).dealOwner(demoAgent).supportAgent(staff)
                .build());

        Deal d4 = dealRepository.save(Deal.builder()
                .code("D26005034")
                .dealName("Gia hạn ACA 2026 - Nhat Huu Tuan Dang (NC)")
                .pipeline("Obamacare 2026")
                .dealStage("New Opportunity/Call to Renew (Obamacare 2026)")
                .carrier("BCBS")
                .planName("Blue Advantage Silver POS")
                .amount(BigDecimal.valueOf(125.50))
                .monthlyPremium(BigDecimal.valueOf(450.00))
                .subsidyAmount(BigDecimal.valueOf(324.50))
                .agencyCommission(BigDecimal.valueOf(90.00))
                .policyId("ACA-7823410")
                .primaryMemberId("MID-98234710")
                .member("Nhat Huu Tuan Dang")
                .numberMember(3).householdSize(4).applicantCount(3)
                .estimatedIncome(BigDecimal.valueOf(45000))
                .sellingState("North Carolina (NC)")
                .enrolledAddress("4301 Laurel Pond Way, Raleigh, NC 27616")
                .quotedCounty("Wake County")
                .enrolledNpn("20011862")
                .paymentStatus("NEED_CHECK_AUTOPAY")
                .payThroughDate(today.plusDays(5))
                .uploadRequest(true).deadlineUpload(today.plusDays(5))
                .chooseDoctorStatus("DONE").doctorName("Dr. Pham Van An")
                .saleSupportStatus("NONE")
                .contact(c4).dealOwner(demoAgent).supportAgent(staff)
                .build());

        Deal d5 = dealRepository.save(Deal.builder()
                .code("D26005035")
                .dealName("Gia hạn ACA 2026 - Ngoc Lan Pham (TX)")
                .pipeline("Obamacare 2026")
                .dealStage("Waiting for document (Obamacare 2026)")
                .carrier("Ambetter")
                .planName("Ambetter Balanced Care 3 Silver")
                .amount(BigDecimal.valueOf(68.00))
                .monthlyPremium(BigDecimal.valueOf(410.00))
                .subsidyAmount(BigDecimal.valueOf(342.00))
                .agencyCommission(BigDecimal.valueOf(75.00))
                .policyId("ACA-5519023")
                .primaryMemberId("MID-66120944")
                .member("Ngoc Lan Pham")
                .numberMember(3).householdSize(3).applicantCount(3)
                .estimatedIncome(BigDecimal.valueOf(38500))
                .sellingState("Texas (TX)")
                .enrolledAddress("3340 W Walnut St, Garland, TX 75042")
                .quotedCounty("Dallas County")
                .enrolledNpn("20011862")
                .paymentStatus("NEED_AUTOPAY")
                .payThroughDate(today.plusDays(7))
                .uploadRequest(true).deadlineUpload(today.plusDays(7))
                .chooseDoctorStatus("DONE").doctorName("Dr. Hoang Thanh Son")
                .saleSupportStatus("NONE")
                .contact(c5).dealOwner(demoAgent).supportAgent(staff)
                .build());

        Deal d6 = dealRepository.save(Deal.builder()
                .code("D26005036")
                .dealName("Gia hạn ACA 2026 - Minh Quan Tran (FL)")
                .pipeline("Obamacare 2026")
                .dealStage("Ready to Enroll (Obamacare 2026)")
                .carrier("Oscar")
                .planName("Oscar Bronze Classic")
                .amount(BigDecimal.valueOf(95.00))
                .monthlyPremium(BigDecimal.valueOf(490.00))
                .subsidyAmount(BigDecimal.valueOf(395.00))
                .agencyCommission(BigDecimal.valueOf(100.00))
                .policyId("ACA-3391842")
                .primaryMemberId("MID-33019822")
                .member("Minh Quan Tran")
                .numberMember(4).householdSize(4).applicantCount(4)
                .estimatedIncome(BigDecimal.valueOf(52000))
                .sellingState("Florida (FL)")
                .enrolledAddress("1820 E Colonial Dr, Orlando, FL 32803")
                .quotedCounty("Orange County")
                .enrolledNpn("20011862")
                .paymentStatus("AUTOPAY")
                .payThroughDate(today.plusDays(12))
                .chooseDoctorStatus("NEED_CHOOSE_DOCTOR")
                .saleSupportStatus("NONE")
                .contact(c6).dealOwner(demoAgent).supportAgent(staff)
                .build());

        Deal d7 = dealRepository.save(Deal.builder()
                .code("D26005037")
                .dealName("ACA Active 2026 - Phuong Thao Vu (TX)")
                .pipeline("Obamacare 2026")
                .dealStage("Enrolled - Active (Obamacare 2026)")
                .carrier("Cigna")
                .planName("Cigna Connect Silver")
                .amount(BigDecimal.valueOf(45.00))
                .monthlyPremium(BigDecimal.valueOf(380.00))
                .subsidyAmount(BigDecimal.valueOf(335.00))
                .agencyCommission(BigDecimal.valueOf(50.00))
                .policyId("ACA-1192837")
                .primaryMemberId("MID-11928370")
                .member("Phuong Thao Vu")
                .numberMember(2).householdSize(2).applicantCount(2)
                .estimatedIncome(BigDecimal.valueOf(34000))
                .sellingState("Texas (TX)")
                .enrolledAddress("11627 S Texas 6, Sugar Land, TX 77498")
                .quotedCounty("Fort Bend County")
                .enrolledNpn("20011862")
                .paymentStatus("AUTOPAY")
                .payThroughDate(today.plusDays(95))
                .chooseDoctorStatus("DONE").doctorName("Dr. Pham Van An")
                .saleSupportStatus("NONE")
                .contact(c7).dealOwner(demoAgent).supportAgent(staff)
                .build());

        Deal d8 = dealRepository.save(Deal.builder()
                .code("D26005038")
                .dealName("Medicare AEP Review - Duc Trong Hoang (CA)")
                .pipeline("Medicare 2026")
                .dealStage("Auto Renew - Active (Medicare 2026)")
                .carrier("UHC")
                .planName("UHC Medicare Advantage Walgreens")
                .amount(BigDecimal.ZERO)
                .monthlyPremium(BigDecimal.ZERO)
                .agencyCommission(BigDecimal.valueOf(306.00))
                .policyId("MC-4401928")
                .primaryMemberId("MBI-9K33M12W")
                .member("Duc Trong Hoang")
                .numberMember(1).householdSize(1).applicantCount(1)
                .estimatedIncome(BigDecimal.valueOf(21000))
                .sellingState("California (CA)")
                .enrolledAddress("2150 Tully Rd, San Jose, CA 95122")
                .quotedCounty("Santa Clara County")
                .enrolledNpn("20011862")
                .paymentStatus("ZERO_PLAN")
                .payThroughDate(today.plusDays(120))
                .chooseDoctorStatus("DONE").doctorName("Dr. Nguyen Minh Tri")
                .saleSupportStatus("NONE")
                .contact(c8).dealOwner(demoAgent).supportAgent(staff)
                .build());

        // ── 4. Seed Tickets ──────────────────────────────────────────────────
        ticketRepository.save(Ticket.builder()
                .code("TC26001001")
                .ticketName("Chuyển đổi ACA sang Medicare (Turning 65) - Mai Thi Tran")
                .pipeline("ACA account")
                .ticketStatus("Waiting on verification")
                .ticketDescription("Khách sắp tròn 65 tuổi trong 18 ngày. Hoàn tất hồ sơ UHC Dual Complete & hủy gói ACA cũ đúng ngày hiệu lực Medicare để không bị trùng/đứt quãng bảo hiểm.")
                .priority("HIGH")
                .dueDate(today.plusDays(3))
                .contact(c1).deal(d1).ticketOwner(demoAgent).serviceAgent(staff)
                .build());

        ticketRepository.save(Ticket.builder()
                .code("TC26001002")
                .ticketName("Thu thập thẻ Medicare Part A/B & SOA - Hung Van Nguyen")
                .pipeline("Upload document")
                .ticketStatus("In Progress")
                .ticketDescription("Khách bước vào kỳ IEP 65 tuổi. Thu thập hình thẻ Red-White-Blue Medicare và chữ ký Scope of Appointment (SOA) trước 48h tư vấn.")
                .priority("HIGH")
                .dueDate(today.plusDays(5))
                .contact(c2).deal(d2).ticketOwner(demoAgent).serviceAgent(staff)
                .build());

        ticketRepository.save(Ticket.builder()
                .code("TC26001003")
                .ticketName("Gia hạn ACA & Xác minh Tax 1040 - Nhat Huu Tuan Dang")
                .pipeline("Upload document")
                .ticketStatus("Waiting on verification")
                .ticketDescription("Hợp đồng BCBS sắp đến hạn gia hạn trong 5 ngày. Cần upload bản khai thuế 1040 để giữ mức trợ cấp Subsidy $324.50/tháng cho gia đình 4 người.")
                .priority("HIGH")
                .dueDate(today.plusDays(2))
                .contact(c4).deal(d4).ticketOwner(demoAgent).serviceAgent(staff)
                .build());

        ticketRepository.save(Ticket.builder()
                .code("TC26001004")
                .ticketName("Ký lại CMS Consent Form gia hạn ACA - Ngoc Lan Pham")
                .pipeline("ACA account")
                .ticketStatus("New")
                .ticketDescription("Hạn gia hạn Ambetter Balanced Care còn 7 ngày. Gửi link SMS xác nhận Consent Form Marketplace 2026 và kiểm tra thẻ Autopay.")
                .priority("HIGH")
                .dueDate(today.plusDays(4))
                .contact(c5).deal(d5).ticketOwner(demoAgent).serviceAgent(staff)
                .build());

        ticketRepository.save(Ticket.builder()
                .code("TC26001005")
                .ticketName("Chọn bác sĩ gia đình (PCP) khi gia hạn ACA - Minh Quan Tran")
                .pipeline("Choose doctor")
                .ticketStatus("In Progress")
                .ticketDescription("Khách đồng ý tái tục gói Oscar Bronze Classic. Hỗ trợ tìm bác sĩ PCP nói tiếng Việt tại khu vực Orlando FL.")
                .priority("MEDIUM")
                .dueDate(today.plusDays(6))
                .contact(c6).deal(d6).ticketOwner(demoAgent).serviceAgent(staff)
                .build());

        // ── 5. Seed Tasks (Nhắc việc tự động cho Agent Demo) ─────────────────
        taskRepository.save(Task.builder()
                .code("TSK26001001")
                .title("[KHẨN] Chốt chuyển đổi Medicare trước sinh nhật 65 tuổi — Bà Lê Thu Hà (Còn 9 ngày)")
                .description("Kiểm tra mã MBI và xác nhận ngày hiệu lực thẻ Aetna Medicare Eagle PPO, nhắc khách hủy Autopay gói ACA cũ.")
                .priority("HIGH").status("IN_PROGRESS").taskType("Call")
                .dueDate(today.plusDays(1))
                .contact(c3).deal(d3).assignedTo(demoAgent).createdBy(demoAgent)
                .build());

        taskRepository.save(Task.builder()
                .code("TSK26001002")
                .title("[Turning 65] Gọi tư vấn gói Medicare UHC — Bà Trần Thị Mai (Còn 18 ngày tròn 65)")
                .description("So sánh quyền lợi Dental/Vision/OTC của UHC Dual Complete so với gói ACA hiện tại.")
                .priority("HIGH").status("OPEN").taskType("Call")
                .dueDate(today.plusDays(2))
                .contact(c1).deal(d1).assignedTo(demoAgent).createdBy(demoAgent)
                .build());

        taskRepository.save(Task.builder()
                .code("TSK26001003")
                .title("[Gia hạn ACA - Còn 5 ngày] Nhắc nộp Tax 1040 giữ Subsidy $324.50 — Đặng Nhật Hữu Tuấn")
                .description("Nếu quá hạn 5 ngày không cập nhật thu nhập trên Marketplace, phí tháng của khách sẽ tăng từ $125.50 lên $450.00.")
                .priority("HIGH").status("OPEN").taskType("Call")
                .dueDate(today.plusDays(2))
                .contact(c4).deal(d4).assignedTo(demoAgent).createdBy(demoAgent)
                .build());

        taskRepository.save(Task.builder()
                .code("TSK26001004")
                .title("[Gia hạn ACA - Còn 7 ngày] Xác nhận Consent Form & thẻ Autopay — Phạm Ngọc Lan")
                .description("Gọi khách xác nhận mức thu nhập dự kiến năm mới $38,500 và tái tục Ambetter Silver.")
                .priority("HIGH").status("OPEN").taskType("Call")
                .dueDate(today.plusDays(3))
                .contact(c5).deal(d5).assignedTo(demoAgent).createdBy(demoAgent)
                .build());

        taskRepository.save(Task.builder()
                .code("TSK26001005")
                .title("[Turning 65] Gửi form Scope of Appointment (SOA) — Ông Nguyễn Văn Hùng")
                .description("Khách còn 42 ngày bước sang tuổi 65. Gửi form SOA điện tử trước buổi hẹn tư vấn Medicare.")
                .priority("MEDIUM").status("OPEN").taskType("Email")
                .dueDate(today.plusDays(5))
                .contact(c2).deal(d2).assignedTo(demoAgent).createdBy(demoAgent)
                .build());

        taskRepository.save(Task.builder()
                .code("TSK26001006")
                .title("[Gia hạn ACA - Còn 12 ngày] Gửi danh sách bác sĩ PCP Việt ngữ — Trần Minh Quân")
                .description("Gửi 3 phòng khám in-network của Oscar tại Orlando FL cho gia đình 4 người.")
                .priority("MEDIUM").status("OPEN").taskType("To Do")
                .dueDate(today.plusDays(6))
                .contact(c6).deal(d6).assignedTo(demoAgent).createdBy(demoAgent)
                .build());

        // ── 6. Seed Notes & Activities ───────────────────────────────────────
        noteRepository.save(Note.builder()
                .text("🎂 CẢNH BÁO TURNING 65: Khách hàng sinh năm 1961, còn 18 ngày tròn 65 tuổi.\nĐã kiểm tra đủ 40 tín chỉ thuế Social Security -> Đủ điều kiện Medicare Part A $0.\nĐề xuất chuyển từ ACA sang UHC Dual Complete Medicare HMO (quyền lợi kính mắt + nha khoa + thẻ mua thuốc OTC $125/tháng).")
                .author("Khanh Nguyen").date("Today").time("09:15 AM")
                .contact(c1).deal(d1).build());

        noteRepository.save(Note.builder()
                .text("⏰ NHẮC GIA HẠN ACA 2026: Hợp đồng BCBS Blue Advantage Silver POS ($125.50/tháng sau trợ cấp $324.50) còn 5 ngày đến hạn xác minh.\nĐã nhắn SMS nhắc anh Tuấn chụp gửi trang đầu Tax 1040 để giữ nguyên mức trợ cấp chính phủ.")
                .author("Khanh Nguyen").date("Today").time("10:30 AM")
                .contact(c4).deal(d4).build());

        activityRepository.save(Activity.builder()
                .type("Turning 65 Medicare Alert")
                .actor("InsurMatch Smart Reminder")
                .time("Today, 08:00")
                .summary("Tự động phát hiện khách hàng Mai Thi Tran sắp bước sang tuổi 65 (còn 18 ngày) — Đã mở hồ sơ chuyển tiếp Medicare IEP")
                .dealTitle(d1.getDealName())
                .linkText("View Medicare Deal")
                .contact(c1).deal(d1).build());

        activityRepository.save(Activity.builder()
                .type("ACA Renewal Alert")
                .actor("InsurMatch Smart Reminder")
                .time("Today, 08:30")
                .summary("Cảnh báo hạn gia hạn ACA còn 5 ngày cho khách hàng Nhat Huu Tuan Dang — Cần xác minh thu nhập giữ Subsidy $324.50/mo")
                .dealTitle(d4.getDealName())
                .linkText("View ACA Deal")
                .contact(c4).deal(d4).build());

        // ── 7. Seed QuoteRequests ────────────────────────────────────────────
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

        // ── 8. Seed Commissions (100% Carrier Payouts Visibility) ────────────
        commissionRepository.save(Commission.builder()
                .agentName("Khanh Nguyen").agentNpn("20011862")
                .carrier("UHC").planName("UHC Dual Complete Medicare HMO")
                .policyId("MC-9920184").memberCount(1)
                .grossAmount(611.0).supportDeduction(0.0).netAmount(611.0)
                .commissionType("MEDICARE_INITIAL").saleSupportStatus("NONE")
                .period("2026-10").status("PENDING")
                .deal(d1).build());

        commissionRepository.save(Commission.builder()
                .agentName("Khanh Nguyen").agentNpn("20011862")
                .carrier("Aetna").planName("Aetna Medicare Eagle PPO")
                .policyId("MC-8841205").memberCount(1)
                .grossAmount(611.0).supportDeduction(0.0).netAmount(611.0)
                .commissionType("MEDICARE_INITIAL").saleSupportStatus("NONE")
                .period("2026-10").status("SETTLED")
                .deal(d3).build());

        commissionRepository.save(Commission.builder()
                .agentName("Khanh Nguyen").agentNpn("20011862")
                .carrier("BCBS").planName("Blue Advantage Silver POS")
                .policyId("ACA-7823410").memberCount(3)
                .grossAmount(90.0).supportDeduction(0.0).netAmount(90.0)
                .commissionType("ACA_PMPM").saleSupportStatus("NONE")
                .period("2026-10").status("SETTLED")
                .deal(d4).build());

        commissionRepository.save(Commission.builder()
                .agentName("Khanh Nguyen").agentNpn("20011862")
                .carrier("Cigna").planName("Cigna Connect Silver")
                .policyId("ACA-1192837").memberCount(2)
                .grossAmount(50.0).supportDeduction(0.0).netAmount(50.0)
                .commissionType("ACA_PMPM").saleSupportStatus("NONE")
                .period("2026-10").status("SETTLED")
                .deal(d7).build());

        // ── 9. Seed Customer Documents & Files ───────────────────────────────
        CustomerDocument doc1 = documentRepository.save(CustomerDocument.builder()
                .code("DOC-2601")
                .name("Mai Thi Tran")
                .initials("MT")
                .contactOwner("Khanh Nguyen (agent@insurmatch.us)")
                .lastModifiedBy("Khanh Nguyen")
                .contact(c1)
                .build());

        fileRepository.save(DocumentFile.builder()
                .document(doc1).category("identity")
                .name("Medicare_Card_PartAB_MaiTran.jpg").fullName("Medicare_Card_PartAB_MaiTran.jpg")
                .size("1.8 MB").type("image").build());

        fileRepository.save(DocumentFile.builder()
                .document(doc1).category("consentFormMkp")
                .name("Scope_Of_Appointment_SOA_Signed.pdf").fullName("Scope_Of_Appointment_SOA_Signed.pdf")
                .size("540 KB").type("pdf").build());

        CustomerDocument doc2 = documentRepository.save(CustomerDocument.builder()
                .code("DOC-2604")
                .name("Nhat Huu Tuan Dang")
                .initials("ND")
                .contactOwner("Khanh Nguyen (agent@insurmatch.us)")
                .lastModifiedBy("Khanh Nguyen")
                .contact(c4)
                .build());

        fileRepository.save(DocumentFile.builder()
                .document(doc2).category("identity")
                .name("Driver_License_NC_TuanDang.jpg").fullName("Driver_License_NC_TuanDang.jpg")
                .size("2.1 MB").type("image").build());

        fileRepository.save(DocumentFile.builder()
                .document(doc2).category("consentFormMkp")
                .name("CMS_Marketplace_Renewal_Consent_2026.pdf").fullName("CMS_Marketplace_Renewal_Consent_2026.pdf")
                .size("480 KB").type("pdf").build());

        fileRepository.save(DocumentFile.builder()
                .document(doc2).category("tax")
                .name("Form_1040_Household_Income_45k.pdf").fullName("Form_1040_Household_Income_45k.pdf")
                .size("1.9 MB").type("pdf").build());

        // ── 9. Seed Subscriptions & PayOS Transactions ──────────────────────
        subscriptionRepository.save(com.insurmatch.entity.Subscription.builder()
                .user(demoAgent)
                .planTier("PROFESSIONAL")
                .billingCycle("MONTHLY")
                .status("ACTIVE")
                .startDate(today.minusDays(15).atStartOfDay())
                .endDate(today.plusDays(45).atStartOfDay())
                .seatsLimit(3)
                .contactsLimit(200)
                .storageLimitGb(25)
                .lastPaymentMethod("VietQR PayOS (1.990.000 VND)")
                .build());

        paymentTransactionRepository.save(com.insurmatch.entity.PaymentTransaction.builder()
                .orderCode(26001099L)
                .user(demoAgent)
                .amount(1_990_000L)
                .currency("VND")
                .description("IM PROFESSIONAL")
                .planTier("PROFESSIONAL")
                .billingCycle("MONTHLY")
                .status("PAID")
                .payosPaymentLinkId("link_demo_payos_01")
                .accountNumber("0838780005")
                .accountName("THE BEST RATE INSURANCE")
                .bankBin("970422")
                .paymentDate(today.minusDays(15).atStartOfDay())
                .build());

        Map<String, Object> result = Map.of(
                "users", 4,
                "contacts", 8,
                "deals", 8,
                "tickets", 5,
                "tasks", 6,
                "quoteRequests", 2,
                "notes", 2,
                "commissions", 4,
                "documents", 2,
                "subscriptions", 1
        );

        return ResponseEntity.ok(ApiResponse.success("Database seeded successfully with 8 rich demo profiles (Turning 65 Medicare & ACA Renewals)", result));
    }
}

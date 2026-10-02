package com.insurmatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Deal — Gói bảo hiểm của khách hàng.
 * Chỉ tạo Deal SAU KHI đã Enrolled thành công (SOP 2).
 */
@Entity
@Table(name = "deals")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deal_name", nullable = false, length = 255)
    private String dealName;

    // ---- Pipeline: OBAMACARE, MEDICARE, MEDICAID_CHIP, TAX, P_AND_C ----
    @Column(nullable = false, length = 30)
    private String pipeline;

    // ---- Deal Stage lifecycle ----
    @Column(name = "deal_stage", nullable = false, length = 50)
    private String dealStage;
    // Stages: READY_TO_ENROLL, ENROLLED, ENROLLED_1ST_PAYMENT_DONE,
    //         ENROLLED_ACTIVE, DO_NOT_CONTACT, TERMINATION, DEAL_LOST,
    //         LOST_SECOND_CHANCE, TERM_SECOND_CHANCE, TELESALES_REVIEW

    // ---- Plan Info ----
    @Column(length = 100)
    private String carrier; // Ambetter, Aetna, BCBS, Molina, Oscar, Cigna, UHC...

    @Column(name = "plan_name", length = 255)
    private String planName;

    @Column
    private BigDecimal amount; // Monthly premium

    @Column(name = "policy_effective_date")
    private LocalDate policyEffectiveDate;

    @Column(name = "policy_id", length = 50)
    private String policyId;

    @Column(name = "member_id", length = 50)
    private String memberId;

    @Column(name = "application_id", length = 50)
    private String applicationId;

    // ---- Household / Applicant ----
    @Column(name = "household_size")
    private Integer householdSize;

    @Column(name = "applicant_count")
    private Integer applicantCount;

    @Column(name = "estimated_income")
    private BigDecimal estimatedIncome;

    @Column(name = "holder_of_policy", length = 150)
    private String holderOfPolicy;

    // ---- NPN & AOR ----
    @Column(name = "enrolled_npn", length = 20)
    private String enrolledNpn;

    @Column(name = "broker_effective_date")
    private LocalDate brokerEffectiveDate; // BED

    @Column(name = "commission_available_date")
    private LocalDate commissionAvailableDate; // CAD

    // ---- Sale Support Status: NONE, PARTIAL, FULL ----
    @Column(name = "sale_support_status", length = 10)
    private String saleSupportStatus;

    // ---- Payment ----
    @Column(name = "payment_status", length = 30)
    private String paymentStatus;
    // NO_VALUE, ZERO_PLAN, SELFPAY, NEED_AUTOPAY, NEED_CHECK_AUTOPAY, COMPANY_PAY, AUTOPAY

    @Column(name = "pay_through_date")
    private LocalDate payThroughDate;

    // ---- Doctor ----
    @Column(name = "choose_doctor_status", length = 30)
    private String chooseDoctorStatus; // NEED_CHOOSE_DOCTOR, DONE

    @Column(name = "doctor_name", length = 150)
    private String doctorName;

    // ---- Upload ----
    @Column(name = "upload_request")
    @Builder.Default
    private Boolean uploadRequest = false;

    @Column(name = "deadline_upload")
    private LocalDate deadlineUpload;

    // ---- Termination ----
    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "termination_reason", length = 500)
    private String terminationReason;

    // ---- FE Specific / Extended Fields ----
    @Column(length = 50)
    private String code;

    @Column(name = "selling_state", length = 100)
    private String sellingState;

    @Column(length = 150)
    private String member;

    @Column(name = "primary_member_id", length = 50)
    private String primaryMemberId;

    @Column(name = "number_member")
    private Integer numberMember;

    @Column(name = "closed_lost_reason", length = 255)
    private String closedLostReason;

    @Column(name = "enrolled_address", length = 255)
    private String enrolledAddress;

    @Column(name = "quoted_county", length = 100)
    private String quotedCounty;

    @Column(name = "is_backdate_deal", length = 10)
    private String isBackdateDeal;

    @Column(name = "close_date", length = 50)
    private String closeDate;

    @Column(name = "monthly_premium")
    private BigDecimal monthlyPremium;

    @Column(name = "subsidy_amount")
    private BigDecimal subsidyAmount;

    @Column(name = "agency_commission")
    private BigDecimal agencyCommission;

    @Column(name = "bonus_tier", length = 50)
    private String bonusTier;

    @Column(name = "payment_option", length = 50)
    private String paymentOption;

    @Column(name = "payment_verification", length = 100)
    private String paymentVerification;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    // ---- Relationships ----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_owner_id")
    private User dealOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_agent_id")
    private User supportAgent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Helper methods for Frontend compatibility ----
    @Transient
    public String getTitle() {
        return dealName;
    }

    public void setTitle(String title) {
        this.dealName = title;
    }

    @Transient
    public String getStage() {
        return dealStage;
    }

    public void setStage(String stage) {
        this.dealStage = stage;
    }

    @Transient
    public String getShortTitle() {
        if (dealName == null) return "";
        return dealName.length() > 25 ? dealName.substring(0, 25) + "..." : dealName;
    }

    @Transient
    public String getDisplayCode() {
        if (code != null && !code.isBlank()) return code;
        return "D2600" + (id != null ? (5000 + id) : "5000");
    }

    @Transient
    public String getContactName() {
        return contact != null ? contact.getFullName() : "";
    }

    @Transient
    public Long getContactId() {
        return contact != null ? contact.getId() : null;
    }

    @Transient
    public String getStageBadge() {
        if (dealStage == null) return "Ready to Enroll";
        if (dealStage.contains("Ready")) return "Ready to Enroll";
        if (dealStage.contains("Active")) return "Active";
        if (dealStage.contains("Enrolled")) return "Enrolled";
        if (dealStage.contains("Lost")) return "Deal Lost";
        if (dealStage.contains("Termination") || dealStage.contains("Term")) return "Terminated";
        return dealStage.length() > 18 ? dealStage.substring(0, 18) : dealStage;
    }

    @Transient
    public String getStageColor() {
        if (dealStage == null) return "bg-blue-50 text-blue-700 border-blue-200";
        if (dealStage.contains("Active")) return "bg-emerald-50 text-emerald-700 border-emerald-200";
        if (dealStage.contains("Ready")) return "bg-blue-50 text-blue-700 border-blue-200";
        if (dealStage.contains("Waiting") || dealStage.contains("Uploaded")) return "bg-amber-50 text-amber-700 border-amber-200";
        if (dealStage.contains("Lost") || dealStage.contains("Term")) return "bg-rose-50 text-rose-700 border-rose-200";
        return "bg-slate-50 text-slate-700 border-slate-200";
    }
}

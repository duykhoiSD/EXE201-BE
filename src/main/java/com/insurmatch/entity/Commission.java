package com.insurmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Commission — Hoa hồng đại lý gắn với Deal.
 */
@Entity
@Table(name = "commissions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Commission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_name", nullable = false, length = 150)
    private String agentName;

    @Column(name = "agent_npn", length = 20)
    @Builder.Default
    private String agentNpn = "";

    @Column(nullable = false, length = 50)
    private String carrier;

    @Column(name = "plan_name", length = 255)
    @Builder.Default
    private String planName = "";

    @Column(name = "policy_id", length = 50)
    @Builder.Default
    private String policyId = "";

    @Column(name = "member_count")
    @Builder.Default
    private Integer memberCount = 1;

    @Column(name = "gross_amount")
    @Builder.Default
    private Double grossAmount = 0.0;

    @Column(name = "support_deduction")
    @Builder.Default
    private Double supportDeduction = 0.0;

    @Column(name = "net_amount")
    @Builder.Default
    private Double netAmount = 0.0;

    @Column(name = "commission_type", length = 30)
    @Builder.Default
    private String commissionType = "ACA_PMPM";

    @Column(name = "sale_support_status", length = 10)
    @Builder.Default
    private String saleSupportStatus = "NONE";

    @Column(nullable = false, length = 20)
    private String period; // e.g. "2026-09"

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, PAID

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private Deal deal;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

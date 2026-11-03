package com.insurmatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Subscription — Quản lý gói dịch vụ SaaS B2B của từng User / Agency.
 * Ma trận hạn mức:
 * - TRIAL: 60 ngày, 1 Seat, 50 Contacts
 * - STARTER: 990.000đ/tháng, 1 Seat, 100 Contacts
 * - PROFESSIONAL: 1.990.000đ/tháng, 3 Seats, 200 Contacts
 * - AGENCY: 4.990.000đ/tháng, 10 Seats, 300 Contacts
 */
@Entity
@Table(name = "subscriptions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "plan_tier", nullable = false, length = 30)
    private String planTier; // TRIAL, STARTER, PROFESSIONAL, AGENCY

    @Column(name = "billing_cycle", length = 20)
    @Builder.Default
    private String billingCycle = "MONTHLY"; // MONTHLY, ANNUAL

    @Column(length = 30)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, GRACE_PERIOD, EXPIRED, CANCELLED

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "seats_limit")
    @Builder.Default
    private Integer seatsLimit = 1;

    @Column(name = "contacts_limit")
    @Builder.Default
    private Integer contactsLimit = 100;

    @Column(name = "storage_limit_gb")
    @Builder.Default
    private Integer storageLimitGb = 2;

    @Column(name = "last_payment_method", length = 100)
    private String lastPaymentMethod;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


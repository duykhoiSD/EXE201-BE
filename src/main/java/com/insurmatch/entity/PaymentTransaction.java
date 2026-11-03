package com.insurmatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * PaymentTransaction — Giao dịch thanh toán gói SaaS qua cổng PayOS (VietQR).
 */
@Entity
@Table(name = "payment_transactions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long amount; // in VND

    @Column(length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(length = 255)
    private String description;

    @Column(name = "plan_tier", nullable = false, length = 30)
    private String planTier; // STARTER, PROFESSIONAL, AGENCY

    @Column(name = "billing_cycle", length = 20)
    @Builder.Default
    private String billingCycle = "MONTHLY"; // MONTHLY, ANNUAL

    @Column(length = 30)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PAID, CANCELLED, FAILED

    @Column(name = "payos_payment_link_id", length = 100)
    private String payosPaymentLinkId;

    @Column(name = "payos_checkout_url", length = 500)
    private String payosCheckoutUrl;

    @Column(name = "payos_qr_code", columnDefinition = "TEXT")
    private String payosQrCode;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_name", length = 150)
    private String accountName;

    @Column(name = "bank_bin", length = 20)
    private String bankBin;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


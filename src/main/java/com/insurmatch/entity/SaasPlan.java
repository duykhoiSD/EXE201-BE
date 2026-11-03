package com.insurmatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * SaasPlan — Bảng danh mục các gói dịch vụ CRM lưu trực tiếp trong Database PostgreSQL.
 * Cho phép chỉnh sửa giá tiền (VNĐ) và hạn mức trực tiếp trên Neon DB.
 */
@Entity
@Table(name = "saas_plans")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SaasPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_code", unique = true, nullable = false, length = 50)
    private String planCode; // TRIAL, STARTER, PROFESSIONAL, AGENCY

    @Column(nullable = false, length = 100)
    private String name; // Starter, Professional, Agency, Trial 60 Ngày

    @Column(name = "target_audience", length = 150)
    private String targetAudience; // Cá nhân đại lý, Đội nhóm nhỏ, Đại lý lớn

    @Column(name = "monthly_price_vnd", nullable = false)
    @Builder.Default
    private Long monthlyPriceVnd = 990000L;

    @Column(name = "annual_price_vnd", nullable = false)
    @Builder.Default
    private Long annualPriceVnd = 9900000L;

    @Column(name = "monthly_price_usd")
    @Builder.Default
    private Integer monthlyPriceUsd = 39;

    @Column(name = "annual_price_usd")
    @Builder.Default
    private Integer annualPriceUsd = 32;

    @Column(name = "max_seats", nullable = false)
    @Builder.Default
    private Integer maxSeats = 1;

    @Column(name = "max_contacts", nullable = false)
    @Builder.Default
    private Integer maxContacts = 100;

    @Column(name = "storage_limit_gb", nullable = false)
    @Builder.Default
    private Integer storageLimitGb = 2;

    @Column(name = "is_popular")
    @Builder.Default
    private Boolean isPopular = false;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "features_json", columnDefinition = "TEXT")
    private String featuresJson;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


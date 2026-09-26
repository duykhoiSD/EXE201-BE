package com.insurmatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * QuoteRequest — Lead từ web form /get-quote.
 * KHÔNG tự động tạo Deal. Admin review và assign Agent.
 */
@Entity
@Table(name = "quote_requests")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QuoteRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(length = 2)
    private String state;

    @Column(name = "insurance_type", length = 30)
    private String insuranceType; // OBAMACARE, MEDICARE, LIFE, P_AND_C, TAX

    @Column(name = "household_size")
    private Integer householdSize;

    @Column(name = "estimated_income")
    private Double estimatedIncome;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ---- Status: NEW, CONTACTED, ASSIGNED, CONVERTED, REJECTED ----
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "NEW";

    // Agent mà Admin assign cho quote này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    // Contact đã được tạo từ quote này (nếu đã convert)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

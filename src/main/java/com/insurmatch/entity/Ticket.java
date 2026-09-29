package com.insurmatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ticket — Xử lý 1 vấn đề cụ thể trên hồ sơ khách hàng (SOP 4).
 * 5 Pipeline: CLIENT_SUPPORT, PAYMENT, COLLECT_DOCUMENT, CHOOSE_DOCTOR, AGENT_SUPPORT
 */
@Entity
@Table(name = "tickets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_name", nullable = false, length = 255)
    private String ticketName;

    // Pipeline: CLIENT_SUPPORT, PAYMENT, COLLECT_DOCUMENT, CHOOSE_DOCTOR, AGENT_SUPPORT, etc.
    @Column(nullable = false, length = 100)
    private String pipeline;

    @Column(name = "ticket_status", nullable = false, length = 100)
    private String ticketStatus;

    @Column(name = "ticket_description", columnDefinition = "TEXT")
    private String ticketDescription;

    @Column(name = "ticket_result", columnDefinition = "TEXT")
    private String ticketResult;

    // Priority: HIGH, MEDIUM, LOW, URGENT, NONE
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String priority = "MEDIUM";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "change_due_date_reason", length = 500)
    private String changeDueDateReason;

    // ---- Relationships ----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id")
    private Deal deal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_owner_id")
    private User ticketOwner; // = Deal Owner (Agent)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_agent_id")
    private User serviceAgent; // = Support chính

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

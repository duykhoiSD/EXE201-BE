package com.insurmatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @Column(name = "code", length = 50)
    private String code;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public String getCode() {
        if (code != null && !code.isBlank()) return code;
        return "TK2600" + (id != null ? (1000 + id) : "1000");
    }

    @Transient
    public String getTitle() {
        return ticketName;
    }

    @Transient
    public String getStatus() {
        return ticketStatus;
    }

    @Transient
    public String getDescription() {
        return ticketDescription;
    }

    @Transient
    public String getContactName() {
        return contact != null ? contact.getFullName() : null;
    }

    @Transient
    public Long getContactId() {
        return contact != null ? contact.getId() : null;
    }

    @Transient
    public String getDealTitle() {
        return deal != null ? deal.getDealName() : null;
    }

    @Transient
    public Long getDealId() {
        return deal != null ? deal.getId() : null;
    }

    @Transient
    private Long ticketOwnerId;

    @Transient
    private String ticketOwnerName;

    @Transient
    private Long serviceAgentId;

    @Transient
    private String serviceAgentName;

    @JsonProperty("ticketOwner")
    public void setTicketOwnerFromJson(Object val) {
        if (val == null) {
            this.ticketOwner = null;
            this.ticketOwnerId = null;
            this.ticketOwnerName = "";
            return;
        }
        if (val instanceof User u) {
            this.ticketOwner = u;
            this.ticketOwnerId = u.getId();
            this.ticketOwnerName = u.getFullName();
        } else if (val instanceof String s) {
            this.ticketOwnerName = s;
        } else if (val instanceof Number n) {
            this.ticketOwnerId = n.longValue();
        } else if (val instanceof java.util.Map<?, ?> map) {
            if (map.get("id") instanceof Number idVal) {
                this.ticketOwnerId = idVal.longValue();
            }
            if (map.get("fullName") instanceof String fn) {
                this.ticketOwnerName = fn;
            } else if (map.get("name") instanceof String n) {
                this.ticketOwnerName = n;
            }
        }
    }

    @JsonProperty("serviceAgent")
    public void setServiceAgentFromJson(Object val) {
        if (val == null) {
            this.serviceAgent = null;
            this.serviceAgentId = null;
            this.serviceAgentName = "";
            return;
        }
        if (val instanceof User u) {
            this.serviceAgent = u;
            this.serviceAgentId = u.getId();
            this.serviceAgentName = u.getFullName();
        } else if (val instanceof String s) {
            this.serviceAgentName = s;
        } else if (val instanceof Number n) {
            this.serviceAgentId = n.longValue();
        } else if (val instanceof java.util.Map<?, ?> map) {
            if (map.get("id") instanceof Number idVal) {
                this.serviceAgentId = idVal.longValue();
            }
            if (map.get("fullName") instanceof String fn) {
                this.serviceAgentName = fn;
            } else if (map.get("name") instanceof String n) {
                this.serviceAgentName = n;
            }
        }
    }

    @Transient
    public String getTicketOwnerName() {
        if (ticketOwnerName != null && !ticketOwnerName.isBlank()) return ticketOwnerName;
        return ticketOwner != null ? ticketOwner.getFullName() : null;
    }

    public void setTicketOwnerName(String ticketOwnerName) {
        this.ticketOwnerName = ticketOwnerName;
    }

    @Transient
    public String getServiceAgentName() {
        if (serviceAgentName != null && !serviceAgentName.isBlank()) return serviceAgentName;
        return serviceAgent != null ? serviceAgent.getFullName() : null;
    }

    public void setServiceAgentName(String serviceAgentName) {
        this.serviceAgentName = serviceAgentName;
    }
}

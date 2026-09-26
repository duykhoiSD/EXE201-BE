package com.insurmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Activity — Ghi nhận Call Log, Meeting, Email trên hồ sơ Contact/Deal.
 */
@Entity
@Table(name = "activities")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    @Builder.Default
    private String type = "Activity";

    @Column(length = 150)
    private String actor;

    @Column(length = 50)
    private String time;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "ticket_title", length = 255)
    @Builder.Default
    private String ticketTitle = "";

    @Column(name = "deal_title", length = 255)
    @Builder.Default
    private String dealTitle = "";

    @Column(name = "link_text", length = 255)
    @Builder.Default
    private String linkText = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id")
    private Deal deal;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

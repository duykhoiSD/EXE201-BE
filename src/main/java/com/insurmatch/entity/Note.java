package com.insurmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Note — Ghi chú gắn với Contact hoặc Deal.
 */
@Entity
@Table(name = "notes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    public void setBody(String body) {
        this.text = body;
    }

    public String getBody() {
        return this.text;
    }

    @Column(length = 150)
    @Builder.Default
    private String author = "System";

    @Column(length = 20)
    private String time;

    @Column(length = 30)
    private String date;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String attachments = "[]";

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

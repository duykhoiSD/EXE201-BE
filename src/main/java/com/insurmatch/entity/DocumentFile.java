package com.insurmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * DocumentFile — File đơn lẻ thuộc về một CustomerDocument.
 */
@Entity
@Table(name = "document_files")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String category; // consentFormMkp, identity, insuranceRecord, paymentInformation, tax...

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(length = 20)
    private String size;

    @Column(length = 20)
    private String type; // pdf, image, document

    @Column(length = 500)
    @Builder.Default
    private String url = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private CustomerDocument document;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

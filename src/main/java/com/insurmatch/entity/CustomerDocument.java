package com.insurmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CustomerDocument — Nhóm tài liệu của khách hàng (Consent, ID, Insurance record...).
 */
@Entity
@Table(name = "customer_documents")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CustomerDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 5)
    @Builder.Default
    private String initials = "ND";

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "contact_owner", length = 150)
    private String contactOwner;

    @Column(name = "last_modified_by", length = 150)
    private String lastModifiedBy;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentFile> files = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @com.fasterxml.jackson.annotation.JsonProperty("code")
    public String getCode() {
        if (code != null && !code.isBlank()) return code;
        return id != null ? "DOC-" + id : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("contactName")
    @Transient
    public String getContactName() {
        return contact != null ? contact.getFullName() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("contactId")
    @Transient
    public String getContactId() {
        if (contact == null) return null;
        return contact.getCode() != null ? contact.getCode() : "CT" + contact.getId();
    }

    @com.fasterxml.jackson.annotation.JsonProperty("associatedContact")
    @Transient
    public java.util.Map<String, Object> getAssociatedContact() {
        if (contact == null) return null;
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", getContactId());
        map.put("name", contact.getFullName());
        map.put("phone", contact.getPhone());
        map.put("email", contact.getEmail());
        map.put("leadOwner", contactOwner != null ? contactOwner : (contact.getContactOwner() != null ? contact.getContactOwner().getFullName() : "Staff"));
        map.put("language", "Vietnamese");
        return map;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("totalFiles")
    @Transient
    public int getTotalFiles() {
        return files != null ? files.size() : 0;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("filesByCategory")
    @Transient
    public java.util.Map<String, List<DocumentFile>> getFilesByCategory() {
        java.util.Map<String, List<DocumentFile>> map = new java.util.HashMap<>();
        String[] cats = {"consentFormMkp", "consentFormText", "identity", "insuranceRecord", "otherDocument", "paymentInformation", "tax"};
        for (String c : cats) {
            map.put(c, new ArrayList<>());
        }
        if (files != null) {
            for (DocumentFile f : files) {
                if (f.getCategory() != null && map.containsKey(f.getCategory())) {
                    map.get(f.getCategory()).add(f);
                } else if (f.getCategory() != null) {
                    map.computeIfAbsent(f.getCategory(), k -> new ArrayList<>()).add(f);
                } else {
                    map.get("otherDocument").add(f);
                }
            }
        }
        return map;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("categoriesSummary")
    @Transient
    public List<java.util.Map<String, Object>> getCategoriesSummary() {
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        java.util.Map<String, List<DocumentFile>> byCat = getFilesByCategory();
        for (java.util.Map.Entry<String, List<DocumentFile>> entry : byCat.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                java.util.Map<String, Object> summary = new java.util.HashMap<>();
                summary.put("key", entry.getKey());
                summary.put("label", toCategoryLabel(entry.getKey()));
                summary.put("count", entry.getValue().size());
                list.add(summary);
            }
        }
        return list;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("lastModifiedTime")
    @Transient
    public String getLastModifiedTime() {
        LocalDateTime time = updatedAt != null ? updatedAt : createdAt;
        if (time == null) return null;
        return time.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy, HH:mm"));
    }

    private String toCategoryLabel(String key) {
        if (key == null) return "Document";
        return switch (key) {
            case "identity" -> "Identity";
            case "consentFormMkp" -> "Consent Form MKP";
            case "consentFormText" -> "Consent Form Text";
            case "paymentInformation" -> "Payment Information";
            case "insuranceRecord" -> "Insurance Record";
            case "tax" -> "Tax";
            default -> "Other Document";
        };
    }
}

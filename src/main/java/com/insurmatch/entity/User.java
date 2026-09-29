package com.insurmatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity — represents Staff, Agent, or Admin users who log into the system.
 */
@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Transient
    public String getFullName() {
        String full = (firstName != null ? firstName : "") + (lastName != null ? " " + lastName : "");
        return full.trim().isEmpty() ? email : full.trim();
    }

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "otp", length = 6)
    private String otp;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(length = 50)
    private String npn;

    @Column(length = 100)
    private String department;

    @Column(name = "states_licensed", length = 255)
    private String statesLicensed;

    @Column(length = 50)
    @Builder.Default
    private String status = "Active";

    @Column(name = "compliance_status", length = 100)
    @Builder.Default
    private String complianceStatus = "Verified & Cleared";

    @Column(name = "suspension_reason", length = 255)
    private String suspensionReason;

    @Column(name = "deals_count")
    @Builder.Default
    private Integer dealsCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getName() {
        String first = firstName != null ? firstName : "";
        String last = lastName != null ? lastName : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? email : full;
    }

    public String getAvatar() {
        String initial = "";
        if (firstName != null && !firstName.isEmpty()) initial += firstName.charAt(0);
        if (lastName != null && !lastName.isEmpty()) initial += lastName.charAt(0);
        if (initial.isEmpty() && email != null && !email.isEmpty()) initial = email.substring(0, Math.min(2, email.length()));
        return initial.toUpperCase();
    }

    public enum Role {
        ADMIN, STAFF, AGENT, MANAGER, SUPPORT, TELESALES
    }
}

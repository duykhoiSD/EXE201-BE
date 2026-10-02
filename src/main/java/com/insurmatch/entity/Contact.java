package com.insurmatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Contact — Hồ sơ khách hàng 360°.
 * Chứa 6 section bắt buộc theo SOP 2: About, Point of Contact, Household,
 * Spouse/Dependent, How do you know us, Customer Document.
 */
@Entity
@Table(name = "contacts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 11)
    private String ssn;

    @Column(length = 50)
    private String gender;

    // ---- Immigration ----
    @Column(name = "immigration_status", length = 50)
    private String immigrationStatus;

    @Column(name = "alien_number", length = 20)
    private String alienNumber;

    @Column(name = "certificate_number", length = 30)
    private String certificateNumber;

    @Column(name = "date_expired")
    private LocalDate dateExpired;

    // ---- Address ----
    @Column(length = 255)
    private String address;

    @Column(name = "mailing_address", length = 255)
    private String mailingAddress;

    @Column(name = "street_address", length = 255)
    private String streetAddress;

    @Column(length = 100)
    private String county;

    @Column(length = 100)
    private String city;

    @Column(length = 2)
    private String state;


    @Column(name = "zip_code", length = 10)
    private String zipCode;

    // ---- Relationship ----
    @Column(length = 20)
    private String relationship; // Self, Spouse, Dependent, etc.

    // ---- Point of Contact ----
    @Column(name = "poc_name", length = 150)
    private String pocName;

    @Column(name = "poc_phone", length = 20)
    private String pocPhone;

    @Column(name = "poc_relationship", length = 50)
    private String pocRelationship;

    // ---- How do you know us ----
    @Column(name = "source_channel", length = 100)
    private String sourceChannel; // Marketing, Event, Referral, Agent, TeleSales, etc.

    @Column(name = "source_detail", length = 255)
    private String sourceDetail;

    @Column(length = 50)
    private String language;

    @Column(length = 50)
    private String status;

    @Column(name = "tele_sale_team", length = 100)
    private String teleSaleTeam;

    @Column(name = "who_refer_client", length = 150)
    private String whoReferClient;

    // ---- Household info ----
    @Column(name = "household_size")
    private Integer householdSize;

    @Column(name = "estimated_income")
    private Double estimatedIncome;

    // ---- ACA Account ----
    @Column(name = "aca_username", length = 100)
    private String acaUsername;

    @Column(name = "aca_password", length = 100)
    private String acaPassword;

    @Column(name = "aca_status", length = 30)
    private String acaStatus; // PENDING, VERIFIED, DONE, NEED_CALL_TO_VERIFIED

    // ---- Ownership ----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_owner_id")
    private User contactOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_agent_id")
    private User supportAgent;

    @Column(name = "ob_share_owner", length = 150)
    private String obShareOwner;

    @Column(name = "medicare_share_owner", length = 150)
    private String medicareShareOwner;

    @Column(name = "life_share_owner", length = 150)
    private String lifeShareOwner;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName.trim());
        if (middleName != null && !middleName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middleName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName.trim());
        }
        return sb.length() > 0 ? sb.toString() : (email != null ? email : "Contact #" + id);
    }

    public String getCode() {
        if (code != null && !code.isBlank()) return code;
        return "CT2600" + (id != null ? (2000 + id) : "0000");
    }
}

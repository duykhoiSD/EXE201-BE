package com.insurmatch.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.insurmatch.config.FlexibleLocalDateDeserializer;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ContactDTO — Khớp 100% với giao diện Frontend (`EXE201-FE`).
 *
 * Cung cấp đầy đủ các trường mà FE mong đợi:
 * - code, fullName, status, language
 * - dateOfBirth / dob (hỗ trợ cả format MM/dd/yyyy và yyyy-MM-dd)
 * - contactOwner: UserSummary { id, name, email, avatar }
 * - howDoYouKnowUs, whoReferClient, teleSaleTeam
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactDTO {

    private Long id;
    private String code;
    private String fullName;

    // ---- About ----
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String phone;

    @JsonAlias({"dob"})
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate dateOfBirth;

    private String ssn;
    private String gender;
    private String language;
    private String status;

    // ---- Immigration ----
    private String immigrationStatus;
    private String alienNumber;
    private String certificateNumber;

    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate dateExpired;

    // ---- Address ----
    private String address;
    private String mailingAddress;
    private String streetAddress;
    private String county;
    private String city;
    private String state;

    @JsonAlias({"postalCode"})
    private String zipCode;

    // ---- Relationship ----
    @JsonAlias({"familyRelationship"})
    private String relationship;

    // ---- Point of Contact ----
    private String pocName;
    private String pocPhone;
    private String pocRelationship;

    // ---- How do you know us ----
    @JsonAlias({"howDoYouKnowUs"})
    private String sourceChannel;

    @JsonAlias({"whoReferClient", "teleSaleTeam"})
    private String sourceDetail;

    // ---- Household info ----
    @JsonAlias({"household"})
    private Integer householdSize;
    private Double estimatedIncome;

    // ---- ACA Account ----
    private String acaUsername;
    private String acaPassword;

    @JsonAlias({"acaAccountStatus"})
    private String acaStatus;

    // ---- Ownership ----
    private Long contactOwnerId;
    private String contactOwnerName;
    private UserSummary contactOwner;

    private Long supportAgentId;
    private String supportAgentName;
    private UserSummary supportAgent;

    private String obShareOwner;
    private String medicareShareOwner;
    private String lifeShareOwner;

    // ---- Timestamps ----
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * UserSummary — Cấu trúc User thu gọn dành cho FE `normalizeContact()`.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
        private String avatar;
    }
}

package com.insurmatch.dto.deal;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDealRequest {

    private String dealName;
    private String title;
    private String pipeline;
    private String stage;
    private String dealStage;
    private String carrier;
    private String sellingState;
    private String planName;
    private String member;
    private String primaryMemberId;
    private String contactName;
    private String contactId;
    private String dealOwner;
    private Long dealOwnerId;
    private String needUpload; // "Yes" | "No"
    private Boolean uploadRequest;
    private BigDecimal amount;
    private String closeDate;
    private String applicationId;
    private BigDecimal estimateHouseholdIncome;
    private Integer householdMember;
    private Integer numberMember;
    private String enrolledAddress;
    private String quotedCounty;
    private String isBackdateDeal;
    private String enrolledNpn;
    private String brokerEffectiveDate;
    private String saleSupportStatus;
    private BigDecimal monthlyPremium;
    private BigDecimal subsidyAmount;
    private BigDecimal agencyCommission;
    private String bonusTier;
}

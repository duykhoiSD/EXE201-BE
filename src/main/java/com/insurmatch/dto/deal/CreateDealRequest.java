package com.insurmatch.dto.deal;

import com.fasterxml.jackson.annotation.JsonSetter;
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
    private String paymentOption;
    private String paymentVerification;
    private String contactPhone;
    private String contactEmail;

    @JsonSetter("amount")
    public void setAmount(Object value) {
        this.amount = parseBigDecimal(value);
    }

    @JsonSetter("estimateHouseholdIncome")
    public void setEstimateHouseholdIncome(Object value) {
        this.estimateHouseholdIncome = parseBigDecimal(value);
    }

    @JsonSetter("monthlyPremium")
    public void setMonthlyPremium(Object value) {
        this.monthlyPremium = parseBigDecimal(value);
    }

    @JsonSetter("subsidyAmount")
    public void setSubsidyAmount(Object value) {
        this.subsidyAmount = parseBigDecimal(value);
    }

    @JsonSetter("agencyCommission")
    public void setAgencyCommission(Object value) {
        this.agencyCommission = parseBigDecimal(value);
    }

    @JsonSetter("householdMember")
    public void setHouseholdMember(Object value) {
        this.householdMember = parseInteger(value);
    }

    @JsonSetter("numberMember")
    public void setNumberMember(Object value) {
        this.numberMember = parseInteger(value);
    }

    private static BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        String s = value.toString().replaceAll("[$,]", "").trim();
        if (s.isEmpty() || s.contains("_") || s.equals("--") || s.equalsIgnoreCase("null")) return null;
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        String s = value.toString().replaceAll("[$,]", "").trim();
        if (s.isEmpty() || s.contains("_") || s.equals("--") || s.equalsIgnoreCase("null")) return null;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }
}

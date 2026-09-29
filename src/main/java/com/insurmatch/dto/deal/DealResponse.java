package com.insurmatch.dto.deal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.insurmatch.entity.Activity;
import com.insurmatch.entity.Deal;
import com.insurmatch.entity.Note;
import com.insurmatch.entity.Task;
import com.insurmatch.entity.Ticket;
import com.insurmatch.entity.User;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealResponse {

    private Long id;
    private String code;
    private String title;
    private String dealName;
    private String shortTitle;
    private String pipeline;
    private String stage;
    private String dealStage;
    private String stageBadge;
    private String stageColor;
    private String carrier;
    private String planName;
    private String amount;
    private String closeDate;
    private String sellingState;
    private String contactName;
    private Long contactId;
    private String member;
    private String primaryMemberId;
    private String numberMember;
    private String applicationId;
    private String estimateHouseholdIncome;
    private String householdMember;
    private String enrolledAddress;
    private String quotedCounty;
    private String isBackdateDeal;
    private String needUpload;
    private Boolean uploadRequest;
    private String monthlyPremium;
    private String subsidyAmount;
    private String agencyCommission;
    private String bonusTier;
    private String saleSupportStatus;
    private String enrolledNpn;
    private String brokerEffectiveDate;
    private String terminationDate;
    private String closedLostReason;

    private DealOwnerDto dealOwner;
    private DealOwnerDto lastModifiedBy;
    private String lastModifiedTime;

    private AdminOnlyDto adminOnly;

    @Builder.Default
    private List<Activity> activities = new ArrayList<>();

    @Builder.Default
    private List<Note> notes = new ArrayList<>();

    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    @Builder.Default
    private List<Map<String, Object>> associatedTickets = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DealOwnerDto {
        private Long id;
        private String name;
        private String avatar;
        private String bg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminOnlyDto {
        private String enrolledNpn;
        private String brokerEffectiveDate;
        private String terminationDate;
        private String leadOwner;
        private String dealOwner;
        private String code;
        private String primaryMemberId;
        private String saleSupportStatus;
        private String numberMember;
        private String sellingState;
        private String carrier;
        private String closedLostReason;
    }

    public static DealResponse fromEntity(Deal deal, List<Activity> activities, List<Note> notes, List<Task> tasks, List<Ticket> tickets) {
        if (deal == null) return null;

        String ownerName = deal.getDealOwner() != null ? deal.getDealOwner().getName() : "Khanh Nguyen";
        String avatar = deal.getDealOwner() != null ? deal.getDealOwner().getAvatar() : "KN";

        DealOwnerDto ownerDto = DealOwnerDto.builder()
                .id(deal.getDealOwner() != null ? deal.getDealOwner().getId() : null)
                .name(ownerName)
                .avatar(avatar)
                .bg("bg-blue-600 text-white")
                .build();

        String amountStr = deal.getAmount() != null ? "$" + deal.getAmount().toString() : "_ _ _ _ _ _ _ _ _ _";

        String numMemberStr = deal.getNumberMember() != null ? String.valueOf(deal.getNumberMember()) :
                (deal.getApplicantCount() != null ? String.valueOf(deal.getApplicantCount()) : "1");

        String displayCode = deal.getDisplayCode();
        String displayTitle = deal.getTitle() != null ? deal.getTitle() : "Deal #" + deal.getId();

        AdminOnlyDto adminOnlyDto = AdminOnlyDto.builder()
                .enrolledNpn(deal.getEnrolledNpn() != null ? deal.getEnrolledNpn() : "")
                .brokerEffectiveDate(deal.getBrokerEffectiveDate() != null ? deal.getBrokerEffectiveDate().toString() : "")
                .terminationDate(deal.getTerminationDate() != null ? deal.getTerminationDate().toString() : "")
                .leadOwner(ownerName)
                .dealOwner(ownerName)
                .code(displayCode)
                .primaryMemberId(deal.getPrimaryMemberId() != null ? deal.getPrimaryMemberId() : "")
                .saleSupportStatus(deal.getSaleSupportStatus() != null ? deal.getSaleSupportStatus() : "None")
                .numberMember(numMemberStr)
                .sellingState(deal.getSellingState() != null ? deal.getSellingState() : "--")
                .carrier(deal.getCarrier() != null ? deal.getCarrier() : "BCBS")
                .closedLostReason(deal.getClosedLostReason() != null ? deal.getClosedLostReason() : "---")
                .build();

        List<Map<String, Object>> ticketDtos = new ArrayList<>();
        if (tickets != null) {
            for (Ticket t : tickets) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId());
                map.put("code", "TC2600" + (1000 + t.getId()));
                map.put("title", t.getTicketName());
                map.put("ticketName", t.getTicketName());
                map.put("pipeline", t.getPipeline());
                map.put("stage", t.getTicketStatus());
                map.put("status", t.getTicketStatus());
                map.put("priority", t.getPriority());
                map.put("dueDate", t.getDueDate() != null ? t.getDueDate().toString() : "");
                map.put("ticketOwner", t.getTicketOwner() != null ? t.getTicketOwner().getName() : ownerName);
                map.put("serviceAgent", t.getServiceAgent() != null ? t.getServiceAgent().getName() : "Platform Staff");
                ticketDtos.add(map);
            }
        }

        return DealResponse.builder()
                .id(deal.getId())
                .code(displayCode)
                .title(displayTitle)
                .dealName(displayTitle)
                .shortTitle(deal.getShortTitle())
                .pipeline(deal.getPipeline() != null ? deal.getPipeline() : "Obamacare 2026")
                .stage(deal.getStage() != null ? deal.getStage() : "Ready to Enroll (Obamacare 2026)")
                .dealStage(deal.getStage() != null ? deal.getStage() : "Ready to Enroll (Obamacare 2026)")
                .stageBadge(deal.getStageBadge())
                .stageColor(deal.getStageColor())
                .carrier(deal.getCarrier() != null ? deal.getCarrier() : "BCBS")
                .planName(deal.getPlanName() != null ? deal.getPlanName() : "")
                .amount(amountStr)
                .closeDate(deal.getCloseDate() != null ? deal.getCloseDate() : "_ _ _ _ _ _ _ _ _ _")
                .sellingState(deal.getSellingState() != null ? deal.getSellingState() : "--")
                .contactName(deal.getContactName())
                .contactId(deal.getContactId())
                .member(deal.getMember() != null ? deal.getMember() : deal.getContactName())
                .primaryMemberId(deal.getPrimaryMemberId() != null ? deal.getPrimaryMemberId() : "")
                .numberMember(numMemberStr)
                .applicationId(deal.getApplicationId() != null ? deal.getApplicationId() : "")
                .estimateHouseholdIncome(deal.getEstimatedIncome() != null ? "$" + deal.getEstimatedIncome() : "")
                .householdMember(deal.getHouseholdSize() != null ? String.valueOf(deal.getHouseholdSize()) : "")
                .enrolledAddress(deal.getEnrolledAddress() != null ? deal.getEnrolledAddress() : "")
                .quotedCounty(deal.getQuotedCounty() != null ? deal.getQuotedCounty() : "")
                .isBackdateDeal(deal.getIsBackdateDeal() != null ? deal.getIsBackdateDeal() : "No")
                .needUpload(deal.getUploadRequest() != null && deal.getUploadRequest() ? "Yes" : "No")
                .uploadRequest(deal.getUploadRequest() != null ? deal.getUploadRequest() : false)
                .monthlyPremium(deal.getMonthlyPremium() != null ? "$" + deal.getMonthlyPremium() : "")
                .subsidyAmount(deal.getSubsidyAmount() != null ? "$" + deal.getSubsidyAmount() : "")
                .agencyCommission(deal.getAgencyCommission() != null ? "$" + deal.getAgencyCommission() : "")
                .bonusTier(deal.getBonusTier() != null ? deal.getBonusTier() : "Standard Tier")
                .saleSupportStatus(deal.getSaleSupportStatus() != null ? deal.getSaleSupportStatus() : "None")
                .enrolledNpn(deal.getEnrolledNpn() != null ? deal.getEnrolledNpn() : "")
                .brokerEffectiveDate(deal.getBrokerEffectiveDate() != null ? deal.getBrokerEffectiveDate().toString() : "")
                .terminationDate(deal.getTerminationDate() != null ? deal.getTerminationDate().toString() : "")
                .closedLostReason(deal.getClosedLostReason() != null ? deal.getClosedLostReason() : "---")
                .dealOwner(ownerDto)
                .lastModifiedBy(DealOwnerDto.builder().name("Platform Staff").avatar("PS").bg("bg-teal-600 text-white").build())
                .lastModifiedTime("Vừa cập nhật")
                .adminOnly(adminOnlyDto)
                .activities(activities != null ? activities : new ArrayList<>())
                .notes(notes != null ? notes : new ArrayList<>())
                .tasks(tasks != null ? tasks : new ArrayList<>())
                .associatedTickets(ticketDtos)
                .build();
    }
}

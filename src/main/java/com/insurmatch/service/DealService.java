package com.insurmatch.service;

import com.insurmatch.entity.Deal;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.DealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DealService {

    private final DealRepository dealRepository;

    public List<Deal> getAllDeals(String search, String stage, String pipeline) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasStage = stage != null && !stage.isBlank();
        boolean hasPipeline = pipeline != null && !pipeline.isBlank();

        if (hasSearch) {
            return dealRepository.searchDeals(search);
        } else if (hasPipeline && hasStage) {
            return dealRepository.findByPipelineAndDealStage(pipeline, stage);
        } else if (hasPipeline) {
            return dealRepository.findByPipeline(pipeline);
        } else if (hasStage) {
            return dealRepository.findByDealStage(stage);
        }
        return dealRepository.findAll();
    }

    public Deal getDealById(Long id) {
        return dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found with id: " + id));
    }

    public Deal updateDeal(Long id, Deal dealData) {
        Deal existing = getDealById(id);
        if (dealData.getDealName() != null) existing.setDealName(dealData.getDealName());
        if (dealData.getPipeline() != null) existing.setPipeline(dealData.getPipeline());
        if (dealData.getDealStage() != null) existing.setDealStage(dealData.getDealStage());
        if (dealData.getCarrier() != null) existing.setCarrier(dealData.getCarrier());
        if (dealData.getPlanName() != null) existing.setPlanName(dealData.getPlanName());
        if (dealData.getAmount() != null) existing.setAmount(dealData.getAmount());
        if (dealData.getPolicyEffectiveDate() != null) existing.setPolicyEffectiveDate(dealData.getPolicyEffectiveDate());
        if (dealData.getPolicyId() != null) existing.setPolicyId(dealData.getPolicyId());
        if (dealData.getMemberId() != null) existing.setMemberId(dealData.getMemberId());
        if (dealData.getPaymentStatus() != null) existing.setPaymentStatus(dealData.getPaymentStatus());
        if (dealData.getPayThroughDate() != null) existing.setPayThroughDate(dealData.getPayThroughDate());
        if (dealData.getChooseDoctorStatus() != null) existing.setChooseDoctorStatus(dealData.getChooseDoctorStatus());
        if (dealData.getDoctorName() != null) existing.setDoctorName(dealData.getDoctorName());
        if (dealData.getTerminationReason() != null) existing.setTerminationReason(dealData.getTerminationReason());
        if (dealData.getTerminationDate() != null) existing.setTerminationDate(dealData.getTerminationDate());
        return dealRepository.save(existing);
    }

    public Deal updateDealAdmin(Long id, Deal adminData) {
        Deal existing = getDealById(id);
        if (adminData.getEnrolledNpn() != null) existing.setEnrolledNpn(adminData.getEnrolledNpn());
        if (adminData.getBrokerEffectiveDate() != null) existing.setBrokerEffectiveDate(adminData.getBrokerEffectiveDate());
        if (adminData.getSaleSupportStatus() != null) existing.setSaleSupportStatus(adminData.getSaleSupportStatus());
        if (adminData.getCommissionAvailableDate() != null) existing.setCommissionAvailableDate(adminData.getCommissionAvailableDate());
        return dealRepository.save(existing);
    }

    public long count() {
        return dealRepository.count();
    }

    public List<Deal> getDealsByContactId(Long contactId) {
        return dealRepository.findByContactId(contactId);
    }
}

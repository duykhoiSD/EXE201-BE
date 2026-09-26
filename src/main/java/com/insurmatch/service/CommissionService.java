package com.insurmatch.service;

import com.insurmatch.entity.Commission;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.CommissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionRepository commissionRepository;

    public List<Commission> getAllCommissions(String agentName, String period, String status, String carrier) {
        boolean hasAny = (agentName != null && !agentName.isBlank())
                || (period != null && !period.isBlank())
                || (status != null && !status.isBlank())
                || (carrier != null && !carrier.isBlank());

        if (hasAny) {
            return commissionRepository.findByFilters(
                    (agentName != null && !agentName.isBlank()) ? agentName : null,
                    (period != null && !period.isBlank()) ? period : null,
                    (status != null && !status.isBlank()) ? status : null,
                    (carrier != null && !carrier.isBlank()) ? carrier : null
            );
        }
        return commissionRepository.findAll();
    }

    public Map<String, Object> getSummary(String agentName) {
        List<Commission> commissions;
        if (agentName != null && !agentName.isBlank()) {
            commissions = commissionRepository.findByAgentName(agentName);
        } else {
            commissions = commissionRepository.findAll();
        }

        double totalGross = commissions.stream().mapToDouble(Commission::getGrossAmount).sum();
        double totalNet = commissions.stream().mapToDouble(Commission::getNetAmount).sum();
        double totalDeduction = commissions.stream().mapToDouble(Commission::getSupportDeduction).sum();
        long pending = commissions.stream().filter(c -> "PENDING".equals(c.getStatus())).count();
        long approved = commissions.stream().filter(c -> "APPROVED".equals(c.getStatus())).count();
        long paid = commissions.stream().filter(c -> "PAID".equals(c.getStatus())).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalGross", totalGross);
        summary.put("totalNet", totalNet);
        summary.put("totalDeduction", totalDeduction);
        summary.put("pending", pending);
        summary.put("approved", approved);
        summary.put("paid", paid);
        summary.put("totalRecords", commissions.size());
        return summary;
    }

    public Commission createCommission(Commission commission) {
        return commissionRepository.save(commission);
    }

    public Commission updateCommission(Long id, Commission data) {
        Commission existing = commissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commission not found with id: " + id));
        if (data.getStatus() != null) existing.setStatus(data.getStatus());
        if (data.getGrossAmount() != null) existing.setGrossAmount(data.getGrossAmount());
        if (data.getNetAmount() != null) existing.setNetAmount(data.getNetAmount());
        if (data.getSupportDeduction() != null) existing.setSupportDeduction(data.getSupportDeduction());
        if (data.getSettledAt() != null) existing.setSettledAt(data.getSettledAt());
        return commissionRepository.save(existing);
    }

    public Map<String, Object> calculateCommissions() {
        // Placeholder: in production this would run commission calculation rules
        List<Commission> all = commissionRepository.findAll();
        Map<String, Object> result = new HashMap<>();
        result.put("processed", all.size());
        result.put("message", "Commission calculation completed");
        return result;
    }
}

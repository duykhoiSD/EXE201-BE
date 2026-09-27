package com.insurmatch.dto.admin;

import com.insurmatch.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String phone;
    private String npn;
    private String department;
    private List<String> statesLicensed;
    private String status;
    private String complianceStatus;
    private String suspensionReason;
    private Integer dealsCount;
    private String avatar;
    private String bg;

    public static AccountResponse fromUser(User user) {
        String roleStr = user.getRole() != null ? user.getRole().name().toLowerCase() : "agent";
        
        List<String> statesList = new ArrayList<>();
        if (user.getStatesLicensed() != null && !user.getStatesLicensed().isBlank()) {
            statesList = Arrays.stream(user.getStatesLicensed().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        String bg = "bg-blue-600 text-white";
        if ("staff".equalsIgnoreCase(roleStr)) {
            bg = "bg-teal-600 text-white";
        } else if ("admin".equalsIgnoreCase(roleStr)) {
            bg = "bg-purple-600 text-white";
        }

        return AccountResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(roleStr)
                .phone(user.getPhone() != null ? user.getPhone() : "")
                .npn(user.getNpn() != null ? user.getNpn() : "")
                .department(user.getDepartment() != null ? user.getDepartment() : "Regional Agent Network")
                .statesLicensed(statesList)
                .status(user.getStatus() != null ? user.getStatus() : "Active")
                .complianceStatus(user.getComplianceStatus() != null ? user.getComplianceStatus() : "Verified & Cleared")
                .suspensionReason(user.getSuspensionReason())
                .dealsCount(user.getDealsCount() != null ? user.getDealsCount() : 0)
                .avatar(user.getAvatar())
                .bg(bg)
                .build();
    }
}

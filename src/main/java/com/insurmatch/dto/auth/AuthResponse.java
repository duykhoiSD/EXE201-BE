package com.insurmatch.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresIn;

    private Map<String, Object> user;

    private String message;

    private String devOtp; // Kèm mã OTP khi ở chế độ dev/test để tiện test nhanh
}

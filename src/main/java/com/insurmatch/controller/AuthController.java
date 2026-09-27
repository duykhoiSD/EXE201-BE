package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.dto.auth.*;
import com.insurmatch.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AuthController — Xác thực và Quản lý phiên:
 *   POST /api/auth/register   : Đăng ký tài khoản qua Email + Tạo mã OTP
 *   POST /api/auth/verify-otp : Xác thực OTP để kích hoạt tài khoản + Trả JWT
 *   POST /api/auth/resend-otp : Gửi lại mã OTP
 *   POST /api/auth/login      : Đăng nhập cấp JWT Token
 *   POST /api/auth/logout     : Đăng xuất
 *   GET  /api/auth/me         : Lấy thông tin tài khoản hiện tại qua JWT
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 1. Đăng ký tài khoản mới qua Email
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response.getMessage(), response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Không thể đăng ký: " + e.getMessage()));
        }
    }

    /**
     * 2. Xác thực mã OTP để kích hoạt tài khoản & cấp JWT Token
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            AuthResponse response = authService.verifyOtp(request);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi xác thực OTP: " + e.getMessage()));
        }
    }

    /**
     * 3. Gửi lại mã OTP
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        try {
            AuthResponse response = authService.resendOtp(request);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi gửi lại OTP: " + e.getMessage()));
        }
    }

    /**
     * 4. Đăng nhập bằng Email & Password (Trả về JWT Token)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "Email hoặc mật khẩu không chính xác"));
        }
    }

    /**
     * 5. Đăng xuất
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    /**
     * 6. Lấy thông tin phiên hiện tại dựa trên JWT Token
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Chưa xác thực hoặc token không hợp lệ"));
        }

        try {
            String email = authentication.getName();
            Map<String, Object> user = authService.getCurrentUser(email);
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thành công", user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Không tìm thấy thông tin tài khoản"));
        }
    }

    /**
     * 7. Quên mật khẩu — Gửi mã OTP xác thực qua email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<AuthResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            AuthResponse response = authService.forgotPassword(request);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 8. Đặt lại mật khẩu mới bằng OTP
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<AuthResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            AuthResponse response = authService.resetPassword(request);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi đặt lại mật khẩu: " + e.getMessage()));
        }
    }

    /**
     * 9. Đổi mật khẩu (dành cho người dùng đã đăng nhập)
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Chưa xác thực hoặc token không hợp lệ"));
        }

        try {
            String email = authentication.getName();
            authService.changePassword(email, request);
            return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công!", null));
        } catch (IllegalArgumentException | org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi đổi mật khẩu: " + e.getMessage()));
        }
    }

    /**
     * 10. Làm mới Access Token bằng Refresh Token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            AuthResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "Refresh token không hợp lệ"));
        }
    }
}

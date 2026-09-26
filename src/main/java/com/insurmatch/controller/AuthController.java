package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.User;
import com.insurmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthController — Khớp với FE authService.js:
 *   POST /api/auth/login
 *   POST /api/auth/logout
 *   GET  /api/auth/me
 *
 * NOTE: Hiện tại dùng session-based đơn giản.
 *       JWT token sẽ được tích hợp đầy đủ ở giai đoạn sau.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email and password are required"));
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid email or password"));
        }

        if (!user.getActive()) {
            return ResponseEntity.status(403).body(ApiResponse.error("Account is deactivated"));
        }

        // Generate a simple token (replace with JWT in production)
        String token = "token-" + user.getRole().name().toLowerCase() + "-" + System.currentTimeMillis();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("name", user.getFirstName() + " " + user.getLastName());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole().name().toLowerCase());
        userInfo.put("avatar", ("" + user.getFirstName().charAt(0) + user.getLastName().charAt(0)).toUpperCase());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userInfo);

        return ResponseEntity.ok(ApiResponse.success("Login successful", result));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Placeholder: parse token and find user
        // For now, return unauthenticated
        if (authHeader == null || authHeader.isBlank()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.success("Authenticated", null));
    }
}

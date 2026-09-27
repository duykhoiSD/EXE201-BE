package com.insurmatch.service;

import com.insurmatch.dto.auth.*;
import com.insurmatch.entity.User;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.UserRepository;
import com.insurmatch.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Đăng ký tài khoản mới qua Email — Tự động sinh OTP 6 số và gửi qua email.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user != null) {
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                throw new IllegalArgumentException("Email đã được đăng ký trong hệ thống. Vui lòng đăng nhập.");
            }
            // User đã đăng ký trước đó nhưng chưa xác thực OTP -> Cho phép cập nhật thông tin và gửi lại OTP
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setPhone(request.getPhone());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            if (request.getNpn() != null) user.setNpn(request.getNpn());
            if (request.getRole() != null) user.setRole(request.getRole());
        } else {
            // Tạo mới user ở trạng thái chờ xác thực (emailVerified = false, active = false)
            user = User.builder()
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phone(request.getPhone())
                    .npn(request.getNpn())
                    .role(request.getRole() != null ? request.getRole() : User.Role.AGENT)
                    .active(false)
                    .emailVerified(false)
                    .build();
        }

        // Sinh mã OTP 6 số ngẫu nhiên
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        userRepository.save(user);

        // Gửi email OTP
        String fullName = user.getFirstName() + " " + user.getLastName();
        emailService.sendOtpEmail(user.getEmail(), otp, fullName);

        return AuthResponse.builder()
                .message("Đăng ký thành công! Mã OTP đã được gửi đến email của bạn (hết hạn sau 5 phút).")
                .devOtp(otp) // Hỗ trợ test nhanh trên dev/Swagger/Postman
                .user(buildUserMap(user))
                .build();
    }

    /**
     * Xác thực mã OTP — Kích hoạt tài khoản và tự động cấp JWT Token để đăng nhập.
     */
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email: " + normalizedEmail));

        if (Boolean.TRUE.equals(user.getEmailVerified()) && user.getOtp() == null) {
            return AuthResponse.builder()
                    .message("Tài khoản này đã được xác thực trước đó. Bạn có thể đăng nhập ngay.")
                    .user(buildUserMap(user))
                    .build();
        }

        if (user.getOtp() == null || !user.getOtp().equals(request.getOtp().trim())) {
            throw new IllegalArgumentException("Mã OTP không chính xác. Vui lòng kiểm tra lại.");
        }

        if (user.getOtpExpiry() != null && user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng bấm 'Gửi lại OTP'.");
        }

        // Kích hoạt tài khoản thành công
        user.setEmailVerified(true);
        user.setActive(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        // Cấp JWT Token ngay sau khi xác thực thành công
        String token = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .message("Xác thực OTP thành công! Tài khoản của bạn đã được kích hoạt.")
                .user(buildUserMap(user))
                .build();
    }

    /**
     * Gửi lại mã OTP mới khi mã cũ hết hạn.
     */
    @Transactional
    public AuthResponse resendOtp(ResendOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email: " + normalizedEmail));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Tài khoản này đã được xác thực. Bạn có thể đăng nhập trực tiếp.");
        }

        String newOtp = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setOtp(newOtp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        String fullName = user.getFirstName() + " " + user.getLastName();
        emailService.sendOtpEmail(user.getEmail(), newOtp, fullName);

        return AuthResponse.builder()
                .message("Mã OTP mới đã được gửi đến email của bạn.")
                .devOtp(newOtp)
                .user(buildUserMap(user))
                .build();
    }

    /**
     * Đăng nhập với Email & Password — Xác thực và cấp JWT Token chuẩn.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không chính xác."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác.");
        }

        // Kiểm tra xem tài khoản đã xác thực OTP chưa
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Tài khoản chưa được xác thực qua email. Vui lòng nhập mã OTP để kích hoạt.");
        }

        // Kiểm tra tài khoản có bị vô hiệu hóa không
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.");
        }

        String token = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .message("Đăng nhập thành công!")
                .user(buildUserMap(user))
                .build();
    }

    /**
     * Quên mật khẩu — Gửi mã OTP khôi phục qua email.
     */
    @Transactional
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email: " + normalizedEmail));

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        String fullName = user.getFirstName() + " " + user.getLastName();
        emailService.sendResetPasswordEmail(user.getEmail(), otp, fullName);

        return AuthResponse.builder()
                .message("Mã OTP khôi phục mật khẩu đã được gửi đến email của bạn (hết hạn sau 5 phút).")
                .devOtp(otp)
                .build();
    }

    /**
     * Đặt lại mật khẩu mới bằng mã OTP.
     */
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email: " + normalizedEmail));

        if (user.getOtp() == null || !user.getOtp().equals(request.getOtp().trim())) {
            throw new IllegalArgumentException("Mã OTP không chính xác. Vui lòng kiểm tra lại.");
        }

        if (user.getOtpExpiry() != null && user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        // Cập nhật mật khẩu mới và xóa mã OTP
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setEmailVerified(true);
        user.setActive(true);
        userRepository.save(user);

        return AuthResponse.builder()
                .message("Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.")
                .build();
    }

    /**
     * Đổi mật khẩu khi đã đăng nhập (cần mật khẩu cũ).
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email: " + normalizedEmail));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không chính xác.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Làm mới token (Refresh Token) khi Access Token hết hạn.
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Refresh token không hợp lệ hoặc đã hết hạn.");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Tài khoản đã bị vô hiệu hóa.");
        }

        String newAccessToken = jwtTokenProvider.generateToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .message("Làm mới token thành công!")
                .user(buildUserMap(user))
                .build();
    }

    /**
     * Lấy thông tin người dùng hiện tại từ email.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return buildUserMap(user);
    }

    private Map<String, Object> buildUserMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getFirstName() + " " + user.getLastName());
        map.put("firstName", user.getFirstName());
        map.put("lastName", user.getLastName());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().name().toLowerCase());
        map.put("roleName", user.getRole().name());
        map.put("active", user.getActive());
        map.put("emailVerified", user.getEmailVerified());
        map.put("phone", user.getPhone());
        map.put("npn", user.getNpn());

        String initial = "";
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            initial += user.getFirstName().charAt(0);
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            initial += user.getLastName().charAt(0);
        }
        map.put("avatar", initial.toUpperCase());

        return map;
    }
}

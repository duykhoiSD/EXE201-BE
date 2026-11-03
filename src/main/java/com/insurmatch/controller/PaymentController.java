package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.PaymentTransaction;
import com.insurmatch.entity.Subscription;
import com.insurmatch.entity.User;
import com.insurmatch.repository.PaymentTransactionRepository;
import com.insurmatch.repository.UserRepository;
import com.insurmatch.service.PayOSService;
import com.insurmatch.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PayOSService payOSService;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    /**
     * 1. Tạo liên kết / mã VietQR thanh toán PayOS
     * POST /api/payments/payos/create-link
     */
    @PostMapping("/payments/payos/create-link")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentLink(@RequestBody Map<String, String> request) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            currentUser = userRepository.findByEmail("agent@insurmatch.us")
                    .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
        }
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng đăng nhập để nâng cấp gói!"));
        }

        String planTier = request.getOrDefault("planTier", "PROFESSIONAL");
        String billingCycle = request.getOrDefault("billingCycle", "MONTHLY");

        Map<String, Object> paymentLinkData = payOSService.createPaymentLink(currentUser, planTier, billingCycle);
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo liên kết thanh toán VietQR PayOS thành công!", paymentLinkData));
    }

    /**
     * 2. Webhook PayOS (IPN) tự động xử lý khi người dùng chuyển khoản thành công
     * POST /api/payments/payos/webhook
     */
    @PostMapping("/payments/payos/webhook")
    public ResponseEntity<Map<String, Object>> handlePayosWebhook(@RequestBody Map<String, Object> payload) {
        log.info("PayOS Webhook triggered: {}", payload);
        boolean success = payOSService.handleWebhook(payload);
        return ResponseEntity.ok(Map.of("error", success ? 0 : 1, "message", success ? "Webhook processed" : "Failed"));
    }

    /**
     * 3. Kiểm tra trạng thái thanh toán của 1 đơn hàng (Polling từ Frontend)
     * GET /api/payments/payos/check/{orderCode}
     */
    @GetMapping("/payments/payos/check/{orderCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkPaymentStatus(@PathVariable Long orderCode) {
        PaymentTransaction tx = paymentTransactionRepository.findByOrderCode(orderCode).orElse(null);
        if (tx == null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("status", "NOT_FOUND", "isPaid", false)));
        }
        boolean isPaid = "PAID".equalsIgnoreCase(tx.getStatus());
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "orderCode", tx.getOrderCode(),
                "status", tx.getStatus(),
                "isPaid", isPaid,
                "planTier", tx.getPlanTier(),
                "amount", tx.getAmount()
        )));
    }

    /**
     * 4. Mô phỏng thanh toán thành công (Dành riêng cho Demo / Test không cần app ngân hàng)
     * POST /api/payments/payos/simulate-success/{orderCode}
     */
    @PostMapping("/payments/payos/simulate-success/{orderCode}")
    public ResponseEntity<ApiResponse<String>> simulatePaymentSuccess(@PathVariable Long orderCode) {
        boolean ok = payOSService.processPaymentSuccess(orderCode);
        if (ok) {
            return ResponseEntity.ok(ApiResponse.success("Mô phỏng thanh toán VietQR thành công! Gói dịch vụ đã được kích hoạt."));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Không thể mô phỏng thanh toán cho đơn hàng: " + orderCode));
    }

    /**
     * 5. Lấy thông tin gói dịch vụ hiện tại & hạn mức của User đăng nhập
     * GET /api/subscriptions/my-plan
     */
    @GetMapping("/subscriptions/my-plan")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMySubscription() {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            currentUser = userRepository.findByEmail("agent@insurmatch.us")
                    .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
        }
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        }

        Subscription sub = subscriptionService.getOrCreateUserSubscription(currentUser);
        Map<String, Object> quota = subscriptionService.checkContactQuota(currentUser);

        Map<String, Object> resp = Map.of(
                "subscription", sub,
                "quota", quota
        );
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    /**
     * 6. Dành riêng cho Admin: Xem toàn bộ danh sách gói mua & doanh thu toàn sàn
     * GET /api/admin/subscriptions
     */
    @GetMapping("/admin/subscriptions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminSubscriptions() {
        Map<String, Object> overview = subscriptionService.getAdminSubscriptionOverview();
        return ResponseEntity.ok(ApiResponse.success(overview));
    }
}

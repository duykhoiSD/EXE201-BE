package com.insurmatch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurmatch.config.PayOSConfig;
import com.insurmatch.entity.PaymentTransaction;
import com.insurmatch.entity.Subscription;
import com.insurmatch.entity.User;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.PaymentTransactionRepository;
import com.insurmatch.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSService {

    private final PayOSConfig payOSConfig;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Gói dịch vụ & Hạn mức nghiệp vụ (Business Rules) ─────────────────────────
    public static final Map<String, Long> MONTHLY_PRICES_VND = Map.of(
            "STARTER", 990_000L,
            "PROFESSIONAL", 1_990_000L,
            "AGENCY", 4_990_000L
    );

    public static final Map<String, Long> ANNUAL_PRICES_VND = Map.of(
            "STARTER", 9_900_000L,      // Tiết kiệm 2 tháng
            "PROFESSIONAL", 19_900_000L,
            "AGENCY", 49_900_000L
    );

    public static final Map<String, Integer> PLAN_SEATS = Map.of(
            "TRIAL", 1,
            "STARTER", 1,
            "PROFESSIONAL", 3,
            "AGENCY", 10
    );

    public static final Map<String, Integer> PLAN_CONTACTS = Map.of(
            "TRIAL", 50,
            "STARTER", 100,
            "PROFESSIONAL", 200,
            "AGENCY", 300
    );

    public static final Map<String, Integer> PLAN_STORAGE_GB = Map.of(
            "TRIAL", 1,
            "STARTER", 2,
            "PROFESSIONAL", 25,
            "AGENCY", 100
    );

    /**
     * Tính số tiền thanh toán (VNĐ) theo gói và chu kỳ
     */
    public long calculatePriceVnd(String planTier, String billingCycle) {
        String cleanPlan = String.valueOf(planTier).trim().toUpperCase();
        String cleanCycle = String.valueOf(billingCycle).trim().toUpperCase();

        if ("ANNUAL".equals(cleanCycle)) {
            return ANNUAL_PRICES_VND.getOrDefault(cleanPlan, 9_900_000L);
        }
        return MONTHLY_PRICES_VND.getOrDefault(cleanPlan, 990_000L);
    }

    /**
     * Tạo liên kết thanh toán PayOS (VietQR) cho gói SaaS
     */
    @Transactional
    public Map<String, Object> createPaymentLink(User user, String planTier, String billingCycle) {
        String cleanPlan = String.valueOf(planTier).trim().toUpperCase();
        String cleanCycle = String.valueOf(billingCycle).trim().toUpperCase();
        long amount = calculatePriceVnd(cleanPlan, cleanCycle);

        // Sinh mã orderCode số nguyên ngẫu nhiên (6 chữ số cuối thời gian + 3 số ngẫu nhiên)
        long orderCode = System.currentTimeMillis() % 1_000_000_000L;

        String description = ("IM " + cleanPlan).substring(0, Math.min(25, ("IM " + cleanPlan).length()));

        String baseUrl = payOSConfig.getFrontendUrl();
        String returnUrl = baseUrl + "/dashboard/agent/subscription?status=success&orderCode=" + orderCode;
        String cancelUrl = baseUrl + "/dashboard/agent/subscription?status=cancelled&orderCode=" + orderCode;

        // Lưu bản ghi giao dịch ở trạng thái PENDING
        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderCode(orderCode)
                .user(user)
                .amount(amount)
                .currency("VND")
                .description(description)
                .planTier(cleanPlan)
                .billingCycle(cleanCycle)
                .status("PENDING")
                .build();

        String checkoutUrl;
        String qrCodeUrl;
        String accountNumber = "0838780005";
        String accountName = "THE BEST RATE INSURANCE";
        String bankBin = "970422"; // MB Bank

        if (payOSConfig.isConfigured()) {
            try {
                // Gọi PayOS API V2: https://api-merchant.payos.vn/v2/payment-requests
                Map<String, Object> reqBody = new LinkedHashMap<>();
                reqBody.put("orderCode", orderCode);
                reqBody.put("amount", amount);
                reqBody.put("description", description);
                reqBody.put("buyerName", user.getFullName());
                reqBody.put("buyerEmail", user.getEmail());
                reqBody.put("buyerPhone", user.getPhone() != null ? user.getPhone() : "0900000000");
                reqBody.put("returnUrl", returnUrl);
                reqBody.put("cancelUrl", cancelUrl);

                // Tính chữ ký số HMAC-SHA256: amount={}&cancelUrl={}&description={}&orderCode={}&returnUrl={}
                String signatureData = String.format("amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                        amount, cancelUrl, description, orderCode, returnUrl);
                String signature = createHmacSha256(signatureData, payOSConfig.getChecksumKey());
                reqBody.put("signature", signature);

                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(payOSConfig.getEndpoint() + "/v2/payment-requests"))
                        .header("x-client-id", payOSConfig.getClientId().trim())
                        .header("x-api-key", payOSConfig.getApiKey().trim())
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reqBody)))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode resJson = objectMapper.readTree(response.body());

                if (response.statusCode() == 200 && "00".equals(resJson.path("code").asText())) {
                    JsonNode dataNode = resJson.path("data");
                    checkoutUrl = dataNode.path("checkoutUrl").asText(returnUrl);
                    qrCodeUrl = dataNode.path("qrCode").asText();
                    accountNumber = dataNode.path("accountNumber").asText(accountNumber);
                    accountName = dataNode.path("accountName").asText(accountName);
                    bankBin = dataNode.path("bin").asText(bankBin);

                    transaction.setPayosPaymentLinkId(dataNode.path("paymentLinkId").asText());
                    transaction.setPayosCheckoutUrl(checkoutUrl);
                    transaction.setPayosQrCode(qrCodeUrl);
                    transaction.setAccountNumber(accountNumber);
                    transaction.setAccountName(accountName);
                    transaction.setBankBin(bankBin);
                } else {
                    log.warn("PayOS API call failed ({}), falling back to VietQR standard: {}", response.statusCode(), response.body());
                    checkoutUrl = generateStandardVietQrUrl(bankBin, accountNumber, accountName, amount, description);
                    qrCodeUrl = checkoutUrl;
                }
            } catch (Exception e) {
                log.warn("PayOS API exception, using VietQR fallback: {}", e.getMessage());
                checkoutUrl = generateStandardVietQrUrl(bankBin, accountNumber, accountName, amount, description);
                qrCodeUrl = checkoutUrl;
            }
        } else {
            // Chế độ Sandbox / Test: Tạo mã VietQR chuẩn Napas 24/7 trực tiếp
            checkoutUrl = generateStandardVietQrUrl(bankBin, accountNumber, accountName, amount, description);
            qrCodeUrl = checkoutUrl;
        }

        transaction.setPayosCheckoutUrl(checkoutUrl);
        transaction.setPayosQrCode(qrCodeUrl);
        transaction.setAccountNumber(accountNumber);
        transaction.setAccountName(accountName);
        transaction.setBankBin(bankBin);
        paymentTransactionRepository.save(transaction);

        Map<String, Object> result = new HashMap<>();
        result.put("orderCode", orderCode);
        result.put("amount", amount);
        result.put("currency", "VND");
        result.put("planTier", cleanPlan);
        result.put("billingCycle", cleanCycle);
        result.put("description", description);
        result.put("checkoutUrl", checkoutUrl);
        result.put("qrCodeUrl", qrCodeUrl);
        result.put("accountNumber", accountNumber);
        result.put("accountName", accountName);
        result.put("bankBin", bankBin);
        result.put("status", "PENDING");
        return result;
    }

    private String generateStandardVietQrUrl(String bin, String accNo, String accName, long amount, String desc) {
        return String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bin, accNo, amount, desc.replace(" ", "%20"), accName.replace(" ", "%20"));
    }

    /**
     * Xử lý webhook PayOS (IPN) khi khách đã thanh toán thành công
     */
    @Transactional
    public boolean handleWebhook(Map<String, Object> webhookPayload) {
        try {
            log.info("Received PayOS Webhook: {}", webhookPayload);
            String code = String.valueOf(webhookPayload.get("code"));

            // PayOS data object chứa orderCode
            Object dataObj = webhookPayload.get("data");
            if (dataObj instanceof Map<?, ?> dataMap) {
                Object orderCodeObj = dataMap.get("orderCode");
                if (orderCodeObj != null) {
                    long orderCode = Long.parseLong(String.valueOf(orderCodeObj));
                    if ("00".equals(code) || "success".equalsIgnoreCase(String.valueOf(webhookPayload.get("desc")))) {
                        return processPaymentSuccess(orderCode);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Error processing PayOS Webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Kích hoạt gói dịch vụ khi thanh toán thành công
     */
    @Transactional
    public boolean processPaymentSuccess(Long orderCode) {
        PaymentTransaction tx = paymentTransactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for orderCode: " + orderCode));

        if ("PAID".equals(tx.getStatus())) {
            return true; // Đã xử lý trước đó
        }

        tx.setStatus("PAID");
        tx.setPaymentDate(LocalDateTime.now());
        paymentTransactionRepository.save(tx);

        User user = tx.getUser();
        String planTier = tx.getPlanTier();
        String cycle = tx.getBillingCycle();

        // Cập nhật hoặc tạo mới Subscription
        Subscription sub = subscriptionRepository.findTopByUserIdOrderByEndDateDesc(user.getId())
                .orElse(Subscription.builder().user(user).build());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentEnd = sub.getEndDate();
        LocalDateTime newStart = (currentEnd != null && currentEnd.isAfter(now)) ? currentEnd : now;
        LocalDateTime newEnd = "ANNUAL".equalsIgnoreCase(cycle) ? newStart.plusDays(365) : newStart.plusDays(30);

        sub.setUser(user);
        sub.setPlanTier(planTier);
        sub.setBillingCycle(cycle);
        sub.setStatus("ACTIVE");
        sub.setStartDate(now);
        sub.setEndDate(newEnd);
        sub.setSeatsLimit(PLAN_SEATS.getOrDefault(planTier, 1));
        sub.setContactsLimit(PLAN_CONTACTS.getOrDefault(planTier, 100));
        sub.setStorageLimitGb(PLAN_STORAGE_GB.getOrDefault(planTier, 2));
        sub.setLastPaymentMethod("VietQR PayOS (" + tx.getAmount() + " VND)");
        subscriptionRepository.save(sub);

        log.info("Successfully activated subscription {} for user {} until {}", planTier, user.getEmail(), newEnd);

        // Gửi email xác nhận hóa đơn nâng cấp
        try {
            String subject = "🎉 [InsurMatch] Xác nhận nâng cấp gói thành công: " + planTier;
            String content = String.format(
                    "<p>Xin chào <strong>%s</strong>,</p>" +
                    "<p>Giao dịch nâng cấp gói dịch vụ <strong>%s</strong> (%s) của bạn đã được thanh toán thành công qua VietQR PayOS.</p>" +
                    "<ul>" +
                    "<li>Mã đơn hàng: <strong>#%d</strong></li>" +
                    "<li>Số tiền: <strong>%d VNĐ</strong></li>" +
                    "<li>Hạn mức hồ sơ: <strong>%d khách hàng</strong></li>" +
                    "<li>Số tài khoản: <strong>%d Seats</strong></li>" +
                    "<li>Thời hạn sử dụng đến: <strong>%s</strong></li>" +
                    "</ul>" +
                    "<p>Cảm ơn bạn đã tin tưởng lựa chọn InsurMatch CRM!</p>",
                    user.getFullName(), planTier, cycle, orderCode, tx.getAmount(),
                    sub.getContactsLimit(), sub.getSeatsLimit(), newEnd.toString());
            emailService.sendHtmlEmail(user.getEmail(), subject, content);
        } catch (Exception e) {
            log.warn("Failed to send subscription confirmation email: {}", e.getMessage());
        }

        return true;
    }

    /**
     * Tiện ích sinh mã HMAC-SHA256 theo chuẩn PayOS
     */
    public static String createHmacSha256(String data, String key) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating HMAC-SHA256", e);
        }
    }
}

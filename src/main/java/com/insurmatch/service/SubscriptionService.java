package com.insurmatch.service;

import com.insurmatch.entity.Subscription;
import com.insurmatch.entity.User;
import com.insurmatch.repository.ContactRepository;
import com.insurmatch.repository.PaymentTransactionRepository;
import com.insurmatch.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ContactRepository contactRepository;

    /**
     * Lấy thông tin gói dịch vụ hiện tại của User (hoặc khởi tạo gói mặc định 60 ngày nếu chưa có)
     */
    @Transactional
    public Subscription getOrCreateUserSubscription(User user) {
        return subscriptionRepository.findTopByUserIdOrderByEndDateDesc(user.getId())
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    Subscription defaultSub = Subscription.builder()
                            .user(user)
                            .planTier("TRIAL")
                            .billingCycle("MONTHLY")
                            .status("ACTIVE")
                            .startDate(now)
                            .endDate(now.plusDays(60))
                            .seatsLimit(1)
                            .contactsLimit(50)
                            .storageLimitGb(1)
                            .lastPaymentMethod("Free 60-Day Trial ($0)")
                            .build();
                    return subscriptionRepository.save(defaultSub);
                });
    }

    /**
     * Kiểm tra và tính toán công thức hạn mức (Quota Metrics):
     * 1. Customer Contacts Quota: usedContacts / maxContacts (VD: 8 / 200)
     * 2. Account Seats: usedSeats / maxSeats (VD: 1 / 3)
     * 3. Next Renewal: Ngày gia hạn tiếp theo (VD: 2026-12-18)
     */
    public Map<String, Object> checkContactQuota(User user) {
        Subscription sub = getOrCreateUserSubscription(user);

        long currentContacts = 0;
        if (user != null && user.getId() != null) {
            currentContacts = contactRepository.countByContactOwnerId(user.getId());
            // Nếu là Admin/Staff hoặc chưa có contact riêng, đếm tổng contact hệ thống
            if (currentContacts == 0 && (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF)) {
                currentContacts = contactRepository.count();
            }
        } else {
            currentContacts = contactRepository.count();
        }

        int limit = sub.getContactsLimit() != null ? sub.getContactsLimit() : 100;
        int seatsLimit = sub.getSeatsLimit() != null ? sub.getSeatsLimit() : 1;
        boolean canAddMore = currentContacts < limit;

        LocalDateTime endDate = sub.getEndDate();
        long daysRemaining = endDate != null
                ? Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), endDate))
                : 60;

        String nextRenewalStr = endDate != null ? endDate.toLocalDate().toString() : "";

        Map<String, Object> result = new HashMap<>();
        result.put("usedContacts", currentContacts);
        result.put("maxContacts", limit);
        result.put("currentCount", currentContacts);
        result.put("limit", limit);
        result.put("usedSeats", 1);
        result.put("maxSeats", seatsLimit);
        result.put("canAddMore", canAddMore);
        result.put("planTier", sub.getPlanTier());
        result.put("billingCycle", sub.getBillingCycle());
        result.put("status", sub.getStatus());
        result.put("nextRenewalDate", nextRenewalStr);
        result.put("daysRemaining", daysRemaining);
        result.put("storageLimitGb", sub.getStorageLimitGb() != null ? sub.getStorageLimitGb() : 2);
        return result;
    }

    /**
     * Báo cáo dành riêng cho Admin: Xem tổng doanh thu và danh sách tất cả thuê bao
     */
    public Map<String, Object> getAdminSubscriptionOverview() {
        List<Subscription> allSubs = subscriptionRepository.findAllByOrderByCreatedAtDesc();
        var allTx = paymentTransactionRepository.findAllByOrderByCreatedAtDesc();

        long totalRevenueVnd = allTx.stream()
                .filter(tx -> "PAID".equalsIgnoreCase(tx.getStatus()))
                .mapToLong(tx -> tx.getAmount() != null ? tx.getAmount() : 0L)
                .sum();

        long paidCount = allTx.stream().filter(tx -> "PAID".equalsIgnoreCase(tx.getStatus())).count();

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalRevenueVnd", totalRevenueVnd);
        overview.put("paidTransactionsCount", paidCount);
        overview.put("totalSubscribers", allSubs.size());
        overview.put("subscriptions", allSubs);
        overview.put("transactions", allTx);
        return overview;
    }
}


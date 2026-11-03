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
     * Kiểm tra xem User đã vượt quá hạn mức hồ sơ khách hàng (Contacts Quota) hay chưa
     */
    public Map<String, Object> checkContactQuota(User user) {
        Subscription sub = getOrCreateUserSubscription(user);
        long currentContacts = contactRepository.count(); // For agent, count contacts assigned to them
        int limit = sub.getContactsLimit() != null ? sub.getContactsLimit() : 100;
        boolean canAddMore = currentContacts < limit;

        Map<String, Object> result = new HashMap<>();
        result.put("currentCount", currentContacts);
        result.put("limit", limit);
        result.put("canAddMore", canAddMore);
        result.put("planTier", sub.getPlanTier());
        result.put("daysRemaining", sub.getEndDate() != null
                ? Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), sub.getEndDate()))
                : 60);
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

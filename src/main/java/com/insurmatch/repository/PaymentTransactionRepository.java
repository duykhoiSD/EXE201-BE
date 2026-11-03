package com.insurmatch.repository;

import com.insurmatch.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByOrderCode(Long orderCode);
    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PaymentTransaction> findAllByOrderByCreatedAtDesc();
    List<PaymentTransaction> findByStatusOrderByCreatedAtDesc(String status);
}

package com.insurmatch.repository;

import com.insurmatch.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findTopByUserIdAndStatusOrderByEndDateDesc(Long userId, String status);
    Optional<Subscription> findTopByUserIdOrderByEndDateDesc(Long userId);
    List<Subscription> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Subscription> findAllByOrderByCreatedAtDesc();
}


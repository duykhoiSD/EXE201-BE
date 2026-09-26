package com.insurmatch.repository;

import com.insurmatch.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByContactIdOrderByCreatedAtDesc(Long contactId);
    List<Activity> findByDealIdOrderByCreatedAtDesc(Long dealId);
}

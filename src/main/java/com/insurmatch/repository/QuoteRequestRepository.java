package com.insurmatch.repository;

import com.insurmatch.entity.QuoteRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {
    List<QuoteRequest> findByStatus(String status);
    List<QuoteRequest> findByAssignedAgentId(Long agentId);
    List<QuoteRequest> findAllByOrderByCreatedAtDesc();
}

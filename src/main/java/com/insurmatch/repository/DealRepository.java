package com.insurmatch.repository;

import com.insurmatch.entity.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long> {

    List<Deal> findByPipeline(String pipeline);

    List<Deal> findByDealStage(String dealStage);

    List<Deal> findByPipelineAndDealStage(String pipeline, String dealStage);

    @Query("SELECT d FROM Deal d WHERE " +
           "LOWER(d.dealName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.carrier) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.planName) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Deal> searchDeals(@Param("search") String search);

    List<Deal> findByContactId(Long contactId);
}

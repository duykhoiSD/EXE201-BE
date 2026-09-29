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

    List<Deal> findByDealOwnerId(Long dealOwnerId);

    @Query("SELECT d FROM Deal d WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(d.dealName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(d.carrier) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(d.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(d.member) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(d.planName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:pipeline IS NULL OR :pipeline = '' OR :pipeline = 'all' OR LOWER(d.pipeline) LIKE LOWER(CONCAT('%', :pipeline, '%'))) AND " +
           "(:stage IS NULL OR :stage = '' OR :stage = 'all' OR LOWER(d.dealStage) LIKE LOWER(CONCAT('%', :stage, '%'))) AND " +
           "(:carrier IS NULL OR :carrier = '' OR :carrier = 'all' OR LOWER(d.carrier) = LOWER(:carrier)) AND " +
           "(:dealOwnerId IS NULL OR d.dealOwner.id = :dealOwnerId)")
    List<Deal> filterDeals(
            @Param("search") String search,
            @Param("pipeline") String pipeline,
            @Param("stage") String stage,
            @Param("carrier") String carrier,
            @Param("dealOwnerId") Long dealOwnerId);

    @Query("SELECT d FROM Deal d WHERE " +
           "LOWER(d.dealName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.carrier) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.planName) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Deal> searchDeals(@Param("search") String search);

    @Query("SELECT d FROM Deal d WHERE d.contact.id = :contactId")
    List<Deal> findByContactId(@Param("contactId") Long contactId);
}

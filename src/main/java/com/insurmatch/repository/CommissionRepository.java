package com.insurmatch.repository;

import com.insurmatch.entity.Commission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Long> {
    List<Commission> findByAgentName(String agentName);
    List<Commission> findByPeriod(String period);
    List<Commission> findByStatus(String status);
    List<Commission> findByCarrier(String carrier);

    @Query("SELECT c FROM Commission c WHERE " +
           "(:agentName IS NULL OR c.agentName = :agentName) AND " +
           "(:period IS NULL OR c.period = :period) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:carrier IS NULL OR c.carrier = :carrier)")
    List<Commission> findByFilters(@Param("agentName") String agentName,
                                    @Param("period") String period,
                                    @Param("status") String status,
                                    @Param("carrier") String carrier);

    List<Commission> findByDealId(Long dealId);
}

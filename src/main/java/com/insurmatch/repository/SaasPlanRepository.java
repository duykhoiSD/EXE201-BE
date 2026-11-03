package com.insurmatch.repository;

import com.insurmatch.entity.SaasPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaasPlanRepository extends JpaRepository<SaasPlan, Long> {

    Optional<SaasPlan> findByPlanCode(String planCode);

    List<SaasPlan> findByIsActiveTrueOrderByMonthlyPriceVndAsc();

    boolean existsByPlanCode(String planCode);
}

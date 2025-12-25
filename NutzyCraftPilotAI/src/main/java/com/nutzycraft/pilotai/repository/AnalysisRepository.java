package com.nutzycraft.pilotai.repository;

import com.nutzycraft.pilotai.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    List<Analysis> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    Analysis findTopByUserIdOrderByCraftScoreDesc(Long userId);
}

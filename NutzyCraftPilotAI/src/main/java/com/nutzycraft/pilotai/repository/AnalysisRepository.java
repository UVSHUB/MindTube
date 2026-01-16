package com.nutzycraft.pilotai.repository;

import com.nutzycraft.pilotai.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {
    List<Analysis> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    long countByUser_Id(UUID userId);

    Analysis findTopByUser_IdOrderByCraftScoreDesc(UUID userId);
}

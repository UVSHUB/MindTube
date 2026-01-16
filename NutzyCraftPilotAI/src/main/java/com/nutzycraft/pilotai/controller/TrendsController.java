package com.nutzycraft.pilotai.controller;

import com.nutzycraft.pilotai.repository.AnalysisRepository;
import com.nutzycraft.pilotai.entity.Analysis;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@RestController
@RequestMapping("/api/trends")
public class TrendsController {

    private final AnalysisRepository analysisRepository;

    public TrendsController(AnalysisRepository analysisRepository) {
        this.analysisRepository = analysisRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getTrends(@RequestParam String userId) {
        UUID userIdUuid = UUID.fromString(userId);
        // Fetch real data
        long totalAnalyses = analysisRepository.countByUser_Id(userIdUuid);
        List<Analysis> recentAnalyses = analysisRepository.findByUser_IdOrderByCreatedAtDesc(userIdUuid);
        Analysis topAnalysis = analysisRepository.findTopByUser_IdOrderByCraftScoreDesc(userIdUuid);

        // Calculate Stats
        int avgCraftScore = 0;
        // int thisWeekCount = 0; // Simplified for MVP (could filter by date)

        if (!recentAnalyses.isEmpty()) {
            avgCraftScore = (int) recentAnalyses.stream()
                    .mapToInt(Analysis::getCraftScore)
                    .average()
                    .orElse(0);

            // Allow simplified "this week" logic: just take count of last 7 if recent?
            // Or just mock the trend "+X" since we don't have historical snapshots easily
            // without more complex queries.
            // We will just calculate avg from all data.
        }

        Map<String, Object> data = new HashMap<>();
        data.put("totalAnalyses", totalAnalyses);
        data.put("thisWeekCount", totalAnalyses > 0 ? "+" + totalAnalyses : "0"); // Just showing total as "new" for now
                                                                                  // if we don't differentiate

        data.put("avgCraftScore", avgCraftScore);
        data.put("craftScoreTrend", "N/A"); // Hard to calc trend without history tables

        if (topAnalysis != null) {
            data.put("topHookScore", topAnalysis.getCraftScore());
            data.put("topHookVideo", topAnalysis.getTitle() != null ? topAnalysis.getTitle() : "Untitled Analysis");
        } else {
            data.put("topHookScore", 0);
            data.put("topHookVideo", "No Data");
        }

        // Chart Data: Last 6 analyses
        List<Map<String, Object>> chart = new ArrayList<>();
        // Take up to 6 most recent, reverse to show oldest -> newest left to right
        List<Analysis> chartSource = recentAnalyses.size() > 6 ? recentAnalyses.subList(0, 6) : recentAnalyses;

        // Reverse for chart order
        for (int i = chartSource.size() - 1; i >= 0; i--) {
            Analysis a = chartSource.get(i);
            Map<String, Object> point = new HashMap<>();
            // Label: use Upload Date or Created At.
            // Format simplified: e.g. "Dec 25"
            String label = a.getCreatedAt().getMonth().name().substring(0, 3) + " " + a.getCreatedAt().getDayOfMonth();

            point.put("label", label);
            point.put("value", a.getCraftScore());
            chart.add(point);
        }

        // Fill with empty if no data? Or let frontend handle empty.

        data.put("chart", chart);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}

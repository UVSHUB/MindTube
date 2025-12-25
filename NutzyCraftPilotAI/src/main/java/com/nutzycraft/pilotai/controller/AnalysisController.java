package com.nutzycraft.pilotai.controller;

import com.nutzycraft.pilotai.entity.Analysis;
import com.nutzycraft.pilotai.entity.User;
import com.nutzycraft.pilotai.repository.AnalysisRepository;
import com.nutzycraft.pilotai.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;

    public AnalysisController(AnalysisRepository analysisRepository, UserRepository userRepository) {
        this.analysisRepository = analysisRepository;
        this.userRepository = userRepository;
    }

    // GET History
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHistory(@RequestParam Long userId) {
        List<Analysis> history = analysisRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // For simple response, we return list directly.
        // Or wrap in success: true
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", history);
        return ResponseEntity.ok(response);
    }

    // DELETE Report
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        analysisRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // POST Analyze (Simulation)
    @PostMapping
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        String url = (String) payload.get("url");

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not found"));
        }

        Analysis analysis = new Analysis(user, url);

        // --- SIMULATE AI ANALYSIS ---
        analysis.setTitle(url.contains("youtube") ? "How to Build a SaaS in 2 Weeks" : "Uploaded Document Analysis");
        analysis.setChannelName("Nutzy Craft");
        analysis.setThumbnailUrl("https://dummyimage.com/320x180/000/fff&text=Video+Thumb");
        analysis.setViews(new Random().nextInt(1000) + "K Views");
        analysis.setUploadDate("2 days ago");

        analysis.setCraftScore(85 + new Random().nextInt(15));
        analysis.setComparison("+" + new Random().nextInt(20) + "% vs avg");
        analysis.setPacingScore(new Random().nextBoolean() ? "High" : "Medium");
        analysis.setClarityScore(8.0 + (new Random().nextDouble() * 2.0));
        analysis.setRetention("+" + new Random().nextInt(60) + "s");
        analysis.setToneMatch("A+");

        analysis.setSummary(
                "<strong>Executive Summary:</strong><br>" +
                        "This content breaks down key strategies for growth. The pacing is excellent, keeping viewers engaged via rapid value delivery.<br><br>"
                        +
                        "<strong>Key Takeaways:</strong>" +
                        "<ul>" +
                        "<li><strong>Value First:</strong> Checks all the boxes for audience retention.</li>" +
                        "<li><strong>Production:</strong> High quality visuals aid understanding.</li>" +
                        "</ul>");

        analysisRepository.save(analysis);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", analysis);
        return ResponseEntity.ok(response);
    }
}

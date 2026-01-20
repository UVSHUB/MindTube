package com.nutzycraft.pilotai.controller;

import com.nutzycraft.pilotai.entity.Analysis;
import com.nutzycraft.pilotai.entity.User;
import com.nutzycraft.pilotai.repository.AnalysisRepository;
import com.nutzycraft.pilotai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);

    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public AnalysisController(AnalysisRepository analysisRepository, UserRepository userRepository) {
        this.analysisRepository = analysisRepository;
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
    }

    // GET History
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHistory(@RequestParam String userId) {
        List<Analysis> history = analysisRepository.findByUser_IdOrderByCreatedAtDesc(UUID.fromString(userId));

        // For simple response, we return list directly.
        // Or wrap in success: true
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", history);
        return ResponseEntity.ok(response);
    }

    // DELETE Report
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable String id) {
        analysisRepository.deleteById(UUID.fromString(id));
        return ResponseEntity.ok().build();
    }

    // POST Analyze - Now integrates with Python AI Service
    @PostMapping
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody Map<String, Object> payload) {
        try {
            UUID userId = UUID.fromString(payload.get("userId").toString());
            String url = (String) payload.get("url");

            // Validate user
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not found"));
            }

            logger.info("Starting analysis for user: {} with URL: {}", userId, url);

            // Call Python AI Service
            AIAnalysisResponse aiResponse = callPythonAIService(url, userId.toString());

            if (aiResponse == null || !aiResponse.success) {
                logger.error("AI Service returned error: {}", aiResponse != null ? aiResponse.error : "null response");
                // Fallback to simulation if AI service fails
                return createSimulatedAnalysis(user, url);
            }

            // Create Analysis entity from AI response
            Analysis analysis = new Analysis(user, url);
            
            // Extract video info (you may want to call YouTube API separately for this)
            analysis.setTitle(extractVideoTitle(url));
            analysis.setChannelName("YouTube Channel"); // Could be enhanced with YouTube API
            analysis.setThumbnailUrl(extractThumbnailUrl(url));
            analysis.setViews("N/A"); // Could be enhanced with YouTube API
            analysis.setUploadDate("Recent"); // Could be enhanced with YouTube API

            // Set AI-generated scores
            analysis.setCraftScore((int) Math.round(aiResponse.craftScore * 10));
            analysis.setClarityScore(aiResponse.hookScore);
            analysis.setPacingScore(getScoreLabel(aiResponse.retentionScore));
            analysis.setRetention(String.format("+%.0f%%", aiResponse.retentionScore * 10));
            analysis.setToneMatch(getGradeLabel(aiResponse.seoScore));
            analysis.setComparison(String.format("+%.0f%% vs avg", (aiResponse.craftScore - 5) * 10));

            // Build comprehensive summary from AI insights
            StringBuilder summaryBuilder = new StringBuilder();
            summaryBuilder.append("<strong>AI-Powered Content Analysis</strong><br><br>");
            
            summaryBuilder.append(String.format("<strong>Summary:</strong><br>%s<br><br>", aiResponse.summary));
            
            summaryBuilder.append("<strong>Strengths:</strong><ul>");
            for (String strength : aiResponse.strengths) {
                summaryBuilder.append(String.format("<li>%s</li>", strength));
            }
            summaryBuilder.append("</ul><br>");
            
            summaryBuilder.append("<strong>Improvements:</strong><ul>");
            for (String improvement : aiResponse.improvements) {
                summaryBuilder.append(String.format("<li>%s</li>", improvement));
            }
            summaryBuilder.append("</ul><br>");
            
            summaryBuilder.append("<strong>SEO Keywords:</strong><br>");
            summaryBuilder.append(String.join(", ", aiResponse.seoKeywords));
            summaryBuilder.append("<br><br>");
            
            if (!aiResponse.titleSuggestions.isEmpty()) {
                summaryBuilder.append("<strong>Title Suggestions:</strong><ol>");
                for (String title : aiResponse.titleSuggestions) {
                    summaryBuilder.append(String.format("<li>%s</li>", title));
                }
                summaryBuilder.append("</ol>");
            }
            
            analysis.setSummary(summaryBuilder.toString());

            // Save to database
            analysisRepository.save(analysis);
            logger.info("Analysis saved successfully for user: {}", userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", analysis);
            response.put("aiScores", Map.of(
                "hookScore", aiResponse.hookScore,
                "retentionScore", aiResponse.retentionScore,
                "seoScore", aiResponse.seoScore,
                "craftScore", aiResponse.craftScore
            ));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during analysis", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Analysis failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Call Python AI Service for content analysis
     */
    private AIAnalysisResponse callPythonAIService(String youtubeUrl, String userId) {
        try {
            String endpoint = aiServiceUrl + "/analyze";
            
            // Prepare request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("youtube_url", youtubeUrl);
            requestBody.put("user_id", userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.info("Calling Python AI Service at: {}", endpoint);

            // Make request to Python service
            ResponseEntity<AIAnalysisResponse> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                request,
                AIAnalysisResponse.class
            );

            AIAnalysisResponse aiResponse = response.getBody();
            logger.info("AI Service response received: {}", aiResponse != null ? "Success" : "Null");
            
            return aiResponse;

        } catch (Exception e) {
            logger.error("Failed to call Python AI Service: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fallback simulation if AI service is unavailable
     */
    private ResponseEntity<Map<String, Object>> createSimulatedAnalysis(User user, String url) {
        logger.warn("Using simulated analysis as fallback");
        
        Analysis analysis = new Analysis(user, url);

        // Simulated values
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
                        "</ul>" +
                        "<br><em>Note: AI Service unavailable - showing simulated results</em>");

        analysisRepository.save(analysis);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", analysis);
        response.put("note", "AI Service unavailable - using simulated data");
        return ResponseEntity.ok(response);
    }

    /**
     * Helper methods for data extraction and formatting
     */
    private String extractVideoTitle(String url) {
        // In production, use YouTube Data API to get actual title
        return "YouTube Video Analysis";
    }

    private String extractThumbnailUrl(String url) {
        // Extract video ID and construct thumbnail URL
        String videoId = extractVideoId(url);
        if (videoId != null) {
            return String.format("https://img.youtube.com/vi/%s/hqdefault.jpg", videoId);
        }
        return "https://dummyimage.com/320x180/000/fff&text=Video";
    }

    private String extractVideoId(String url) {
        // Simple regex to extract YouTube video ID
        if (url.contains("youtube.com/watch?v=")) {
            int start = url.indexOf("watch?v=") + 8;
            int end = url.indexOf("&", start);
            if (end == -1) end = url.length();
            return url.substring(start, end);
        } else if (url.contains("youtu.be/")) {
            int start = url.indexOf("youtu.be/") + 9;
            int end = url.indexOf("?", start);
            if (end == -1) end = url.length();
            return url.substring(start, end);
        }
        return null;
    }

    private String getScoreLabel(double score) {
        if (score >= 8.5) return "Excellent";
        if (score >= 7.0) return "High";
        if (score >= 5.5) return "Medium";
        return "Low";
    }

    private String getGradeLabel(double score) {
        if (score >= 9.0) return "A+";
        if (score >= 8.0) return "A";
        if (score >= 7.0) return "B+";
        if (score >= 6.0) return "B";
        if (score >= 5.0) return "C";
        return "D";
    }

    /**
     * DTO for Python AI Service Response
     */
    public static class AIAnalysisResponse {
        public double hookScore;
        public double retentionScore;
        public double seoScore;
        public double craftScore;
        public List<String> strengths;
        public List<String> improvements;
        public List<String> seoKeywords;
        public List<String> titleSuggestions;
        public String summary;
        public boolean success;
        public String error;

        // Getters and setters
        public double getHookScore() { return hookScore; }
        public void setHookScore(double hookScore) { this.hookScore = hookScore; }
        
        public double getRetentionScore() { return retentionScore; }
        public void setRetentionScore(double retentionScore) { this.retentionScore = retentionScore; }
        
        public double getSeoScore() { return seoScore; }
        public void setSeoScore(double seoScore) { this.seoScore = seoScore; }
        
        public double getCraftScore() { return craftScore; }
        public void setCraftScore(double craftScore) { this.craftScore = craftScore; }
        
        public List<String> getStrengths() { return strengths; }
        public void setStrengths(List<String> strengths) { this.strengths = strengths; }
        
        public List<String> getImprovements() { return improvements; }
        public void setImprovements(List<String> improvements) { this.improvements = improvements; }
        
        public List<String> getSeoKeywords() { return seoKeywords; }
        public void setSeoKeywords(List<String> seoKeywords) { this.seoKeywords = seoKeywords; }
        
        public List<String> getTitleSuggestions() { return titleSuggestions; }
        public void setTitleSuggestions(List<String> titleSuggestions) { this.titleSuggestions = titleSuggestions; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}

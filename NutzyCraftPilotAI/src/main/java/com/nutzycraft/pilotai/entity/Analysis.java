package com.nutzycraft.pilotai.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analysis_reports", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Schema fields
    @Column(name = "source_url")
    private String sourceUrl; // Maps to videoUrl in application logic

    @Column(name = "title")
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "craft_score")
    private Integer craftScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Additional fields stored in metrics JSONB column
    // These are not direct columns but can be stored in the metrics JSONB field
    // videoUrl is an alias for sourceUrl for backward compatibility
    public String getVideoUrl() {
        return sourceUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.sourceUrl = videoUrl;
    }

    @Transient
    private String thumbnailUrl;

    @Transient
    private String channelName;

    @Transient
    private String views;

    @Transient
    private String uploadDate;

    @Transient
    private String comparison;

    @Transient
    private String pacingScore;

    @Transient
    private Double clarityScore;

    @Transient
    private String retention;

    @Transient
    private String toneMatch;

    // Constructor for quick creation
    public Analysis(User user, String videoUrl) {
        this.user = user;
        this.sourceUrl = videoUrl;
        this.createdAt = LocalDateTime.now();
    }
}

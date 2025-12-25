package com.nutzycraft.pilotai.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "analyses", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String videoUrl;

    // Extracted/Mocked Data
    private String title;
    private String thumbnailUrl;
    private String channelName;
    private String views;
    private String uploadDate;

    // Scores
    private int craftScore;
    private String comparison; // e.g. "+12% vs avg"
    private String pacingScore; // e.g. "High"
    private double clarityScore; // e.g. 8.5
    private String retention; // e.g. "+40s"
    private String toneMatch; // e.g. "A+"

    @Column(columnDefinition = "TEXT")
    private String summary; // HTML/Text summary

    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructor for quick creation
    public Analysis(User user, String videoUrl) {
        this.user = user;
        this.videoUrl = videoUrl;
        this.createdAt = LocalDateTime.now();
    }
}

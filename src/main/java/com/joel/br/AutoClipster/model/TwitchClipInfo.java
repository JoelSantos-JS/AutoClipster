package com.joel.br.AutoClipster.model;

import com.joel.br.AutoClipster.enums.ProcessingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "twitch_clip_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwitchClipInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String clipId; // ID único do clipe na Twitch

    @ManyToOne
    @JoinColumn(name = "channel_id", nullable = false)
    private MonitoredChannel channel;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String thumbnailUrl;

    @Column(nullable = false)
    private Integer duration; // Em segundos

    @Column(nullable = false)
    private Integer viewCount;

    @Column(nullable = false)
    private String gameName;

    @Column(nullable = false)
    private String creatorName;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime discoveredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status = ProcessingStatus.DISCOVERED;

    private Double viralScore; // Score calculado pela IA (0-10)

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis; // JSON com análise da IA

    private LocalDateTime processedAt;
}
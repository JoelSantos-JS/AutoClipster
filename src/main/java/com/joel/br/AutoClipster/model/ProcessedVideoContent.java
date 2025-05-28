package com.joel.br.AutoClipster.model;

import com.joel.br.AutoClipster.enums.Platform;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_video_content")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedVideoContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "clip_id", nullable = false)
    private TwitchClipInfo clip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(nullable = false)
    private String generatedTitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String generatedDescription;

    @Column(columnDefinition = "TEXT")
    private String generatedHashtags;

    @Column(columnDefinition = "TEXT")
    private String generatedHook; // Frase para os primeiros segundos

    @Column(nullable = false)
    private String processedVideoPath; // Caminho do arquivo processado

    @Column(nullable = false)
    private String videoFormat; // mp4, mov, etc.

    @Column(nullable = false)
    private Integer videoWidth;

    @Column(nullable = false)
    private Integer videoHeight;

    @Column(nullable = false)
    private Integer videoDuration; // Em segundos

    @Column(nullable = false)
    private Long fileSizeBytes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String aiPromptUsed; // Prompt usado para gerar o conteúdo

    @Column(columnDefinition = "TEXT")
    private String processingLogs; // Logs do processamento

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
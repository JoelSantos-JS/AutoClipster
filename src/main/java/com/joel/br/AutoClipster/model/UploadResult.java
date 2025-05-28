package com.joel.br.AutoClipster.model;

import com.joel.br.AutoClipster.enums.Platform;
import com.joel.br.AutoClipster.enums.UploadStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "upload_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "processed_content_id", nullable = false)
    private ProcessedVideoContent processedContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadStatus status;

    private String platformVideoId; // ID do vídeo na plataforma de destino
    private String platformUrl; // URL do vídeo publicado

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime scheduledFor; // Para uploads agendados

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // Em caso de erro

    @Column(columnDefinition = "TEXT")
    private String uploadLogs; // Logs detalhados do upload

    private Integer retryAttempts = 0;
    private LocalDateTime lastRetryAt;

    // Métricas de performance (atualizadas posteriormente)
    private Long views = 0L;
    private Long likes = 0L;
    private Long comments = 0L;
    private Long shares = 0L;
    private Double engagementRate = 0.0;

    private LocalDateTime lastMetricsUpdate;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
package com.joel.br.AutoClipster.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "youtube_videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouTubeVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "youtube_id", unique = true, length = 50)
    private String youtubeId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON array como string

    @Column(name = "category_id", length = 10)
    private String categoryId;

    @Column(name = "privacy_status", length = 20)
    @Enumerated(EnumType.STRING)
    private PrivacyStatus privacyStatus;

    @Column(name = "upload_status", length = 20)
    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus;

    @Column(name = "channel_id", length = 100)
    private String channelId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clip_id", nullable = false)
    @JsonIgnore
    private DownloadedClip clip;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "watch_url", length = 500)
    private String watchUrl;

    // Estatísticas do vídeo
    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "like_count")
    @Builder.Default
    private Long likeCount = 0L;

    @Column(name = "comment_count")
    @Builder.Default
    private Long commentCount = 0L;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    // Metadados de upload
    @Column(name = "upload_progress")
    @Builder.Default
    private Integer uploadProgress = 0;

    @Column(name = "upload_error", columnDefinition = "TEXT")
    private String uploadError;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    // Timestamps
    @Column(name = "upload_started_at")
    private LocalDateTime uploadStartedAt;

    @Column(name = "upload_completed_at")
    private LocalDateTime uploadCompletedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "stats_updated_at")
    private LocalDateTime statsUpdatedAt;

    // Configurações avançadas
    @Column(name = "auto_levels")
    @Builder.Default
    private Boolean autoLevels = false;

    @Column(name = "stabilize")
    @Builder.Default
    private Boolean stabilize = false;

    @Column(name = "made_for_kids")
    @Builder.Default
    private Boolean madeForKids = false;

    @Column(name = "self_declared_made_for_kids")
    @Builder.Default
    private Boolean selfDeclaredMadeForKids = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PrivacyStatus {
        PRIVATE("private"),
        UNLISTED("unlisted"),
        PUBLIC("public");

        private final String value;

        PrivacyStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static PrivacyStatus fromValue(String value) {
            for (PrivacyStatus status : values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            return PRIVATE; // Default
        }
    }

    public enum UploadStatus {
        PENDING("pending"),
        UPLOADING("uploading"), 
        PROCESSING("processing"),
        COMPLETED("completed"),
        FAILED("failed"),
        REJECTED("rejected"),
        DUPLICATE("duplicate");

        private final String value;

        UploadStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static UploadStatus fromValue(String value) {
            for (UploadStatus status : values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            return PENDING; // Default
        }
    }

    /**
     * Verifica se o upload foi bem-sucedido
     */
    public boolean isUploadSuccessful() {
        return uploadStatus == UploadStatus.COMPLETED && youtubeId != null;
    }

    /**
     * Verifica se o upload falhou
     */
    public boolean isUploadFailed() {
        return uploadStatus == UploadStatus.FAILED || uploadStatus == UploadStatus.REJECTED;
    }

    /**
     * Verifica se pode tentar novamente o upload
     */
    public boolean canRetry() {
        return isUploadFailed() && retryCount < maxRetries;
    }

    /**
     * Incrementa o contador de tentativas
     */
    public void incrementRetryCount() {
        this.retryCount = (retryCount == null ? 0 : retryCount) + 1;
    }

    /**
     * Marca o upload como iniciado
     */
    public void markUploadStarted() {
        this.uploadStatus = UploadStatus.UPLOADING;
        this.uploadStartedAt = LocalDateTime.now();
        this.uploadProgress = 0;
    }

    /**
     * Marca o upload como completado
     */
    public void markUploadCompleted(String youtubeId) {
        this.youtubeId = youtubeId;
        this.uploadStatus = UploadStatus.COMPLETED;
        this.uploadCompletedAt = LocalDateTime.now();
        this.uploadProgress = 100;
        this.uploadError = null;
    }

    /**
     * Marca o upload como falhou
     */
    public void markUploadFailed(String error) {
        this.uploadStatus = UploadStatus.FAILED;
        this.uploadError = error;
        this.uploadProgress = 0;
    }

    /**
     * Atualiza estatísticas do vídeo
     */
    public void updateStats(Long views, Long likes, Long comments) {
        this.viewCount = views;
        this.likeCount = likes;
        this.commentCount = comments;
        this.statsUpdatedAt = LocalDateTime.now();
    }

    /**
     * Gera URL completa para assistir o vídeo
     */
    public String getFullWatchUrl() {
        if (youtubeId != null) {
            return "https://www.youtube.com/watch?v=" + youtubeId;
        }
        return watchUrl;
    }
} 
// Modelo DownloadedClip para armazenar informações sobre clips baixados
package com.joel.br.AutoClipster.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "downloaded_clips")
@AllArgsConstructor
@NoArgsConstructor
public class DownloadedClip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String clipId;

    private String title;
    private Integer viewCount;
    private String creatorName;
    private String broadcasterName;
    private LocalDateTime downloadDate;
    private String filePath;
    private String gameName;
    private Double duration;
    private String originalUrl;

    @Column(nullable = false)
    private boolean processed;

    // Status opcional para tracking
    private String processingStatus;

    // YouTubeId será preenchido após o upload
    private String youtubeId;
}
package com.joel.br.AutoClipster.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado do workflow de processamento automático
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowResult {
    private String channelName;
    private String channelId;
    private Integer clipsDownloaded;
    private Integer clipsProcessed;
    private Integer clipsUploaded;
    private String status; // "COMPLETED", "FAILED", "IN_PROGRESS"
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<String> processedClipTitles;
    private Double averageViralScore;
    private Integer totalEstimatedViews;
} 
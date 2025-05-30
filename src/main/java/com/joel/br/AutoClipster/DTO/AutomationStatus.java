package com.joel.br.AutoClipster.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Status geral do sistema de automação
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AutomationStatus {
    private Long totalClipsDownloaded;
    private Long totalClipsProcessed;
    private Long totalClipsPending;
    private Long totalClipsFailed;
    private Long totalClipsSkipped;
    private boolean isProcessingActive;
    private LocalDateTime lastProcessedAt;
    private LocalDateTime systemStartedAt;
    private String currentWorkflowStatus;
    private Integer activeWorkflows;
    private Double averageProcessingTime; // em segundos
} 
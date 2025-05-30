package com.joel.br.AutoClipster.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para requisições de upload no YouTube
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouTubeUploadRequest {
    
    /**
     * ID do clip a ser uploadado
     */
    private Long clipId;
    
    /**
     * ID do usuário
     */
    private String userId;
    
    /**
     * Título customizado (opcional)
     */
    private String title;
    
    /**
     * Descrição customizada (opcional)
     */
    private String description;
    
    /**
     * Tags customizadas (opcional)
     */
    private String tags;
    
    /**
     * Status de privacidade (private, unlisted, public)
     */
    private String privacyStatus;
    
    /**
     * ID da categoria do YouTube
     */
    private String categoryId;
    
    /**
     * Se é feito para crianças
     */
    private Boolean madeForKids;
    
    /**
     * Auto ajuste de níveis
     */
    private Boolean autoLevels;
    
    /**
     * Estabilização de vídeo
     */
    private Boolean stabilize;
    
    /**
     * Se deve sobrescrever upload existente
     */
    private Boolean forceUpload;
    
    /**
     * Se monetização está habilitada
     */
    private Boolean monetizationEnabled;
    
    /**
     * Data/hora agendada para publicação
     */
    private LocalDateTime schedulePublishAt;
} 
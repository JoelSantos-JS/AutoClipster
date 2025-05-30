package com.joel.br.AutoClipster.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.joel.br.AutoClipster.model.YouTubeVideo;

import java.time.LocalDateTime;

/**
 * DTO para respostas de upload no YouTube
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouTubeUploadResponse {
    
    /**
     * Se o upload foi bem-sucedido
     */
    private boolean success;
    
    /**
     * Mensagem de resultado
     */
    private String message;
    
    /**
     * ID do vídeo no banco de dados
     */
    private Long videoId;
    
    /**
     * ID do vídeo no YouTube (quando disponível)
     */
    private String youtubeId;
    
    /**
     * URL do vídeo no YouTube (quando disponível)
     */
    private String watchUrl;
    
    /**
     * Status atual do upload
     */
    private String uploadStatus;
    
    /**
     * Progresso do upload (0-100)
     */
    private Integer uploadProgress;
    
    /**
     * Erro de upload (se houver)
     */
    private String uploadError;
    
    /**
     * Timestamp da resposta
     */
    private LocalDateTime timestamp;
    
    /**
     * Dados completos do vídeo (opcional)
     */
    private YouTubeVideo video;
    
    /**
     * Construtor para sucesso
     */
    public static YouTubeUploadResponse success(String message, YouTubeVideo video) {
        return YouTubeUploadResponse.builder()
            .success(true)
            .message(message)
            .videoId(video.getId())
            .youtubeId(video.getYoutubeId())
            .watchUrl(video.getWatchUrl())
            .uploadStatus(video.getUploadStatus().getValue())
            .uploadProgress(video.getUploadProgress())
            .video(video)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Construtor para erro
     */
    public static YouTubeUploadResponse error(String message, String error) {
        return YouTubeUploadResponse.builder()
            .success(false)
            .message(message)
            .uploadError(error)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Construtor para erro com vídeo
     */
    public static YouTubeUploadResponse error(String message, YouTubeVideo video) {
        return YouTubeUploadResponse.builder()
            .success(false)
            .message(message)
            .videoId(video != null ? video.getId() : null)
            .uploadStatus(video != null ? video.getUploadStatus().getValue() : null)
            .uploadError(video != null ? video.getUploadError() : null)
            .uploadProgress(video != null ? video.getUploadProgress() : null)
            .video(video)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Construtor para falha (alias para error)
     */
    public static YouTubeUploadResponse failure(String message) {
        return error(message, (String) null);
    }
    
    /**
     * Construtor para sucesso com informações básicas
     */
    public static YouTubeUploadResponse success(String message, String youtubeId, String watchUrl) {
        return YouTubeUploadResponse.builder()
            .success(true)
            .message(message)
            .youtubeId(youtubeId)
            .watchUrl(watchUrl)
            .uploadStatus("completed")
            .uploadProgress(100)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Construtor para erro simples (sobrecarga para erro sem vídeo)
     */
    public static YouTubeUploadResponse error(String message) {
        return error(message, (String) null);
    }
} 
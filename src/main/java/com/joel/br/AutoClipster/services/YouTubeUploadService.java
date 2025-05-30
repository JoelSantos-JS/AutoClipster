package com.joel.br.AutoClipster.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.joel.br.AutoClipster.DTO.YouTubeUploadRequest;
import com.joel.br.AutoClipster.DTO.YouTubeUploadResponse;
import com.joel.br.AutoClipster.config.YouTubeConfig;
import com.joel.br.AutoClipster.model.DownloadedClip;
import com.joel.br.AutoClipster.model.YouTubeVideo;
import com.joel.br.AutoClipster.model.YouTubeVideo.UploadStatus;
import com.joel.br.AutoClipster.model.YouTubeVideo.PrivacyStatus;
import com.joel.br.AutoClipster.repository.DownloadedClipRepository;
import com.joel.br.AutoClipster.repository.YouTubeVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class YouTubeUploadService {

    private final YouTubeAuthService authService;
    private final YouTubeVideoRepository videoRepository;
    private final DownloadedClipRepository clipRepository;
    private final YouTubeConfig youTubeConfig;
    private final RateLimitService rateLimitService;

    @Value("${youtube.auto-upload:false}")
    private boolean autoUploadEnabled;

    private static final String VIDEO_MIME_TYPE = "video/*";
    private static final String RATE_LIMIT_KEY = "youtube-upload";

    /**
     * Upload de vídeo para o YouTube
     */
    @Async
    @Transactional
    public CompletableFuture<YouTubeUploadResponse> uploadVideo(YouTubeUploadRequest request) {
        try {
            log.info("Iniciando upload para YouTube - Clip ID: {}", request.getClipId());

            // Verificar rate limit
            if (rateLimitService.isRateLimited(RATE_LIMIT_KEY)) {
                return CompletableFuture.completedFuture(
                    YouTubeUploadResponse.failure("Rate limit excedido para uploads do YouTube")
                );
            }
            
            try {
                rateLimitService.acquirePermission(RATE_LIMIT_KEY, 1, java.time.Duration.ofMinutes(1));
            } catch (Exception e) {
                return CompletableFuture.completedFuture(
                    YouTubeUploadResponse.failure("Rate limit excedido: " + e.getMessage())
                );
            }

            // Buscar clip
            Optional<DownloadedClip> clipOpt = clipRepository.findById(request.getClipId());
            if (clipOpt.isEmpty()) {
                return CompletableFuture.completedFuture(
                    YouTubeUploadResponse.failure("Clip não encontrado: " + request.getClipId())
                );
            }

            DownloadedClip clip = clipOpt.get();

            // Verificar se já foi feito upload
            Optional<YouTubeVideo> existingVideo = videoRepository.findByDownloadedClipId(clip.getId());
            if (existingVideo.isPresent() && 
                existingVideo.get().getUploadStatus() == UploadStatus.COMPLETED) {
                
                YouTubeVideo video = existingVideo.get();
                return CompletableFuture.completedFuture(
                    YouTubeUploadResponse.success("Upload já realizado", video)
                );
            }

            // Obter credenciais válidas
            Optional<Credential> credentialOpt = authService.getValidCredential(request.getUserId());
            if (credentialOpt.isEmpty()) {
                return CompletableFuture.completedFuture(
                    YouTubeUploadResponse.failure("Credenciais do YouTube não encontradas ou expiradas")
                );
            }

            // Realizar upload
            YouTubeVideo video = performUpload(clip, request, credentialOpt.get());
            
            // Salvar resultado
            video = videoRepository.save(video);

            log.info("Upload concluído com sucesso - YouTube ID: {}", video.getYoutubeId());
            return CompletableFuture.completedFuture(
                convertToResponse(video)
            );

        } catch (Exception e) {
            log.error("Erro durante upload do YouTube: ", e);
            return CompletableFuture.completedFuture(
                YouTubeUploadResponse.failure("Erro durante upload: " + e.getMessage())
            );
        }
    }

    /**
     * Executa o upload propriamente dito
     */
    private YouTubeVideo performUpload(DownloadedClip clip, YouTubeUploadRequest request, Credential credential) 
            throws Exception {

        // Criar ou atualizar registro do vídeo
        YouTubeVideo video = videoRepository.findByDownloadedClipId(clip.getId())
                .orElse(new YouTubeVideo());

        video.setClip(clip);
        video.setTitle(sanitizeTitle(request.getTitle()));
        video.setDescription(request.getDescription());
        if (request.getTags() != null) {
            video.setTags(request.getTags());
        }
        video.setUploadStatus(UploadStatus.UPLOADING);
        video.setPrivacyStatus(PrivacyStatus.valueOf(request.getPrivacyStatus().toUpperCase()));
        video.setCategoryId(request.getCategoryId());

        video = videoRepository.save(video);

        try {
            // Construir serviço do YouTube
            YouTube youtubeService = youTubeConfig.buildYouTubeService(credential);

            // Preparar metadados do vídeo
            Video videoMetadata = new Video();
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus(request.getPrivacyStatus());
            status.setEmbeddable(true);
            status.setLicense("youtube");
            videoMetadata.setStatus(status);

            VideoSnippet snippet = new VideoSnippet();
            snippet.setTitle(video.getTitle());
            snippet.setDescription(video.getDescription());
            if (request.getTags() != null) {
                snippet.setTags(Arrays.asList(request.getTags().split(",")));
            }
            snippet.setCategoryId(request.getCategoryId());
            
            if (request.getSchedulePublishAt() != null) {
                status.setPublishAt(new com.google.api.client.util.DateTime(
                    request.getSchedulePublishAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                ));
            }

            videoMetadata.setSnippet(snippet);

            // Preparar arquivo para upload
            File videoFile = new File(clip.getFilePath());
            if (!videoFile.exists()) {
                throw new RuntimeException("Arquivo de vídeo não encontrado: " + clip.getFilePath());
            }

            InputStreamContent mediaContent = new InputStreamContent(VIDEO_MIME_TYPE, new FileInputStream(videoFile));
            mediaContent.setLength(videoFile.length());

            // Executar upload
            YouTube.Videos.Insert videoInsert = youtubeService.videos()
                    .insert(Arrays.asList("snippet", "statistics", "status"), videoMetadata, mediaContent);

            // Executar upload
            Video uploadedVideo = videoInsert.execute();

            // Atualizar informações do vídeo
            video.setYoutubeId(uploadedVideo.getId());
            video.setVideoUrl(String.format(YouTubeConfig.YouTubeConstants.VIDEO_URL_TEMPLATE, uploadedVideo.getId()));
            video.setThumbnailUrl(String.format(YouTubeConfig.YouTubeConstants.THUMBNAIL_URL_TEMPLATE, uploadedVideo.getId()));
            video.setUploadStatus(UploadStatus.PROCESSING);
            video.setPublishedAt(LocalDateTime.now());

            // Atualizar clip original
            clip.setYoutubeId(uploadedVideo.getId());
            clipRepository.save(clip);

            log.info("Upload realizado com sucesso - ID: {} para clip: {}", uploadedVideo.getId(), clip.getTitle());

            return video;

        } catch (Exception e) {
            log.error("Erro durante upload: ", e);
            video.markUploadFailed(e.getMessage());
            video.incrementRetryCount();
            videoRepository.save(video);
            throw e;
        }
    }

    /**
     * Upload automático baseado em análise do Gemini
     */
    @Async
    public CompletableFuture<YouTubeUploadResponse> autoUploadFromAnalysis(Long clipId, String userId) {
        if (!autoUploadEnabled) {
            return CompletableFuture.completedFuture(
                YouTubeUploadResponse.error("Upload automático desabilitado", "AUTO_UPLOAD_DISABLED")
            );
        }

        try {
            Optional<DownloadedClip> clipOpt = clipRepository.findById(clipId);
            if (clipOpt.isEmpty()) {
                return CompletableFuture.completedFuture(
                    YouTubeUploadResponse.error("Clip não encontrado", "CLIP_NOT_FOUND")
                );
            }

            DownloadedClip clip = clipOpt.get();

            // Verificar se clip tem análise
            if (!clip.isProcessed()) {
                return CompletableFuture.completedFuture(
                    YouTubeUploadResponse.error("Clip ainda não foi analisado pelo Gemini", "CLIP_NOT_ANALYZED")
                );
            }

            // Buscar metadados do clip (assumindo que foram gerados pelo Gemini)
            YouTubeUploadRequest request = YouTubeUploadRequest.builder()
                    .clipId(clipId)
                    .userId(userId)
                    .title(clip.getTitle())
                    .description("Clip de " + clip.getCreatorName())
                    .tags(String.join(",", Arrays.asList(clip.getGameName(), clip.getCreatorName(), "gaming")))
                    .build();

            return uploadVideo(request);

        } catch (Exception e) {
            log.error("Erro no upload automático: ", e);
            return CompletableFuture.completedFuture(
                YouTubeUploadResponse.error("Erro no upload automático: " + e.getMessage(), "AUTO_UPLOAD_ERROR")
            );
        }
    }

    /**
     * Retry de uploads falhados
     */
    @Transactional
    public YouTubeUploadResponse retryFailedUploads(int maxRetries) {
        try {
            List<YouTubeVideo> failedVideos = videoRepository.findFailedVideosForRetry();
            
            if (failedVideos.isEmpty()) {
                return YouTubeUploadResponse.builder()
                        .success(true)
                        .message("Nenhum vídeo falhado encontrado para reprocessamento")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            int retriedCount = 0;
            for (YouTubeVideo video : failedVideos) {
                if (retriedCount >= maxRetries) {
                    break;
                }
                
                if (video.canRetry()) {
                    log.info("Tentando novamente upload do vídeo: {}", video.getTitle());
                    
                    // Criar request para retry
                    YouTubeUploadRequest retryRequest = YouTubeUploadRequest.builder()
                            .clipId(video.getClip().getId())
                            .userId(video.getUserId())
                            .title(video.getTitle())
                            .description(video.getDescription())
                            .tags(video.getTags())
                            .privacyStatus(video.getPrivacyStatus().getValue())
                            .categoryId(video.getCategoryId())
                            .build();
                    
                    uploadVideo(retryRequest);
                    retriedCount++;
                }
            }

            return YouTubeUploadResponse.builder()
                    .success(true)
                    .message(String.format("Reprocessamento iniciado para %d vídeos", retriedCount))
                    .timestamp(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Erro ao reprocessar uploads falhados: ", e);
            return YouTubeUploadResponse.error("Erro ao reprocessar uploads: " + e.getMessage());
        }
    }

    /**
     * Atualiza estatísticas de um vídeo já publicado
     */
    public void updateVideoStatistics(String youtubeId) {
        try {
            Optional<YouTubeVideo> videoOpt = videoRepository.findByYoutubeId(youtubeId);
            if (videoOpt.isEmpty()) {
                return;
            }

            YouTubeVideo video = videoOpt.get();
            
            // Buscar credenciais válidas
            Optional<String> userId = authService.getAuthenticatedUsers().stream()
                    .map(cred -> cred.getUserId())
                    .findFirst();

            if (userId.isEmpty()) {
                return;
            }

            Optional<Credential> credentialOpt = authService.getValidCredential(userId.get());
            if (credentialOpt.isEmpty()) {
                return;
            }

            YouTube youtubeService = youTubeConfig.buildYouTubeService(credentialOpt.get());
            
            VideoListResponse response = youtubeService.videos()
                    .list(Arrays.asList("statistics"))
                    .setId(Arrays.asList(youtubeId))
                    .execute();

            if (!response.getItems().isEmpty()) {
                Video youtubeVideo = response.getItems().get(0);
                VideoStatistics stats = youtubeVideo.getStatistics();

                if (stats != null) {
                    Long views = stats.getViewCount() != null ? stats.getViewCount().longValue() : 0L;
                    Long likes = stats.getLikeCount() != null ? stats.getLikeCount().longValue() : 0L;
                    Long comments = stats.getCommentCount() != null ? stats.getCommentCount().longValue() : 0L;
                    
                    video.updateStats(views, likes, comments);
                    videoRepository.save(video);
                    log.info("Estatísticas atualizadas para vídeo: {}", youtubeId);
                }
            }

        } catch (Exception e) {
            log.error("Erro ao atualizar estatísticas do vídeo {}: ", youtubeId, e);
        }
    }

    /**
     * Converte YouTubeVideo para YouTubeUploadResponse
     */
    private YouTubeUploadResponse convertToResponse(YouTubeVideo video) {
        return YouTubeUploadResponse.builder()
                .success(video.isUploadSuccessful())
                .message(video.isUploadSuccessful() ? "Upload realizado com sucesso" : "Upload falhou")
                .videoId(video.getId())
                .youtubeId(video.getYoutubeId())
                .watchUrl(video.getWatchUrl())
                .uploadStatus(video.getUploadStatus().getValue())
                .uploadProgress(video.getUploadProgress())
                .uploadError(video.getUploadError())
                .timestamp(LocalDateTime.now())
                .video(video)
                .build();
    }

    /**
     * Sanitiza título para YouTube
     */
    private String sanitizeTitle(String title) {
        if (title == null) return "Clip Épico";
        
        // Remove caracteres inválidos e limita tamanho
        String sanitized = title.replaceAll("[<>]", "").trim();
        
        if (sanitized.length() > YouTubeConfig.YouTubeConstants.MAX_TITLE_LENGTH) {
            sanitized = sanitized.substring(0, YouTubeConfig.YouTubeConstants.MAX_TITLE_LENGTH - 3) + "...";
        }
        
        return sanitized.isEmpty() ? "Clip Épico" : sanitized;
    }
} 
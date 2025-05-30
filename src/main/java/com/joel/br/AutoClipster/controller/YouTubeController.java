package com.joel.br.AutoClipster.controller;

import com.joel.br.AutoClipster.DTO.YouTubeUploadRequest;
import com.joel.br.AutoClipster.DTO.YouTubeUploadResponse;
import com.joel.br.AutoClipster.model.YouTubeCredentials;
import com.joel.br.AutoClipster.model.YouTubeVideo;
import com.joel.br.AutoClipster.repository.YouTubeVideoRepository;
import com.joel.br.AutoClipster.services.YouTubeAuthService;
import com.joel.br.AutoClipster.services.YouTubeUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
@Slf4j
public class YouTubeController {

    private final YouTubeAuthService authService;
    private final YouTubeUploadService uploadService;
    private final YouTubeVideoRepository videoRepository;

    /**
     * Inicia o processo de autenticação OAuth do YouTube
     */
    @PostMapping("/auth/start")
    public ResponseEntity<Map<String, Object>> startAuth(@RequestParam String userId) {
        try {
            log.info("Iniciando autenticação YouTube para usuário: {}", userId);
            
            String authUrl = authService.startAuthFlow(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("authUrl", authUrl);
            response.put("message", "Acesse a URL para autorizar o AutoClipster");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao iniciar autenticação: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Endpoint de callback para completar a autenticação OAuth
     */
    @GetMapping("/auth/callback")
    public ResponseEntity<Map<String, Object>> authCallback(
            @RequestParam String code,
            @RequestParam String state) {
        
        try {
            log.info("Completando autenticação YouTube para usuário: {}", state);
            
            YouTubeCredentials credentials = authService.completeAuthFlow(code, state);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Autenticação concluída com sucesso!");
            response.put("channelTitle", credentials.getChannelTitle());
            response.put("channelId", credentials.getChannelId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao completar autenticação: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Upload de vídeo para o YouTube
     */
    @PostMapping("/upload")
    public CompletableFuture<ResponseEntity<YouTubeUploadResponse>> uploadVideo(
            @RequestBody YouTubeUploadRequest request) {
        
        log.info("Recebida solicitação de upload - Clip ID: {}", request.getClipId());
        
        return uploadService.uploadVideo(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Erro no upload: ", throwable);
                    return ResponseEntity.badRequest()
                            .body(YouTubeUploadResponse.failure(throwable.getMessage()));
                });
    }

    /**
     * Upload automático baseado em análise do Gemini
     */
    @PostMapping("/upload/auto")
    public CompletableFuture<ResponseEntity<YouTubeUploadResponse>> autoUpload(
            @RequestParam Long clipId,
            @RequestParam String userId) {
        
        log.info("Iniciando upload automático - Clip ID: {}, User: {}", clipId, userId);
        
        return uploadService.autoUploadFromAnalysis(clipId, userId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Erro no upload automático: ", throwable);
                    return ResponseEntity.badRequest()
                            .body(YouTubeUploadResponse.failure(throwable.getMessage()));
                });
    }

    /**
     * Lista vídeos por status de upload
     */
    @GetMapping("/videos/status/{status}")
    public ResponseEntity<List<YouTubeVideo>> getVideosByStatus(@PathVariable String status) {
        try {
            YouTubeVideo.UploadStatus uploadStatus = YouTubeVideo.UploadStatus.fromValue(status);
            List<YouTubeVideo> videos = videoRepository.findByUploadStatus(uploadStatus);
            return ResponseEntity.ok(videos);
        } catch (Exception e) {
            log.error("Erro ao buscar vídeos por status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obter informações de um vídeo específico
     */
    @GetMapping("/videos/{youtubeId}")
    public ResponseEntity<YouTubeVideo> getVideo(@PathVariable String youtubeId) {
        return videoRepository.findByYoutubeId(youtubeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retry de uploads falhados
     */
    @PostMapping("/retry-failed")
    public ResponseEntity<Map<String, Object>> retryFailedUploads(
            @RequestParam(defaultValue = "3") int maxRetries) {
        
        try {
            uploadService.retryFailedUploads(maxRetries);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reprocessamento de uploads falhados iniciado");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao reprocessar uploads: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Atualiza estatísticas de um vídeo
     */
    @PostMapping("/videos/{youtubeId}/update-stats")
    public ResponseEntity<Map<String, Object>> updateVideoStats(@PathVariable String youtubeId) {
        try {
            uploadService.updateVideoStatistics(youtubeId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estatísticas atualizadas com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao atualizar estatísticas: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Status das credenciais de autenticação
     */
    @GetMapping("/auth/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(@RequestParam String userId) {
        Map<String, Object> response = new HashMap<>();
        
        boolean hasValidCredentials = authService.hasValidCredentials(userId);
        response.put("authenticated", hasValidCredentials);
        response.put("userId", userId);
        
        if (hasValidCredentials) {
            // Buscar informações do canal
            // Note: Implementação simplificada
            response.put("message", "Usuário autenticado com sucesso");
        } else {
            response.put("message", "Usuário não autenticado ou credenciais expiradas");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Lista todos os usuários autenticados
     */
    @GetMapping("/auth/users")
    public ResponseEntity<List<YouTubeCredentials>> getAuthenticatedUsers() {
        List<YouTubeCredentials> users = authService.getAuthenticatedUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Revoga credenciais de um usuário
     */
    @DeleteMapping("/auth/revoke")
    public ResponseEntity<Map<String, Object>> revokeAuth(@RequestParam String userId) {
        try {
            authService.revokeCredentials(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Credenciais revogadas com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao revogar credenciais: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Estatísticas gerais dos uploads
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUploadStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Contadores por status
        for (YouTubeVideo.UploadStatus status : YouTubeVideo.UploadStatus.values()) {
            Long count = videoRepository.countByUploadStatus(status);
            stats.put(status.name().toLowerCase() + "Count", count);
        }
        
        // Estatísticas gerais
        stats.put("totalVideos", videoRepository.count());
        stats.put("activeUsers", authService.getAuthenticatedUsers().size());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Teste de conectividade com YouTube API
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestParam String userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean hasCredentials = authService.hasValidCredentials(userId);
            
            response.put("success", hasCredentials);
            response.put("message", hasCredentials ? 
                "Conexão com YouTube API funcionando" : 
                "Credenciais não encontradas ou inválidas");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro no teste de conexão: ", e);
            
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
} 
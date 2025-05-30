package com.joel.br.AutoClipster.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joel.br.AutoClipster.model.DownloadedClip;
import com.joel.br.AutoClipster.model.N8nWebhookPayload;
import com.joel.br.AutoClipster.model.YouTubeVideo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class N8nWebhookService {

    private final ObjectMapper objectMapper;
    private final RateLimitService rateLimitService;
    private final RestTemplate restTemplate;

    @Value("${n8n.webhook.url:}")
    private String webhookUrl;

    @Value("${n8n.enabled:false}")
    private boolean n8nEnabled;

    @Value("${n8n.timeout.seconds:30}")
    private int timeoutSeconds;

    @Value("${n8n.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${n8n.retry.delay-seconds:5}")
    private int retryDelaySeconds;

    private static final String RATE_LIMIT_KEY = "n8n-webhook";

    /**
     * Verifica se o serviço está habilitado e configurado
     */
    public boolean isEnabled() {
        return n8nEnabled && webhookUrl != null && !webhookUrl.trim().isEmpty();
    }

    /**
     * Notifica quando um clip é baixado
     */
    @Async
    public CompletableFuture<Boolean> notifyClipDownloaded(DownloadedClip clip) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(false);
        }

        N8nWebhookPayload payload = N8nWebhookPayload.clipDownloaded(clip);
        return sendWebhook(payload);
    }

    /**
     * Notifica quando um clip é analisado
     */
    @Async
    public CompletableFuture<Boolean> notifyClipAnalyzed(DownloadedClip clip, String optimizedTitle, 
                                                        String optimizedDescription, String[] tags) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(false);
        }

        N8nWebhookPayload payload = N8nWebhookPayload.clipAnalyzed(clip, optimizedTitle, optimizedDescription, tags);
        return sendWebhook(payload);
    }

    /**
     * Notifica quando um vídeo é enviado para o YouTube
     */
    @Async
    public CompletableFuture<Boolean> notifyYouTubeUploaded(DownloadedClip clip, YouTubeVideo video) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(false);
        }

        N8nWebhookPayload payload = N8nWebhookPayload.youtubeUploaded(clip, video);
        return sendWebhook(payload);
    }

    /**
     * Notifica quando um workflow é completado
     */
    @Async
    public CompletableFuture<Boolean> notifyWorkflowCompleted(DownloadedClip clip, YouTubeVideo video) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(false);
        }

        N8nWebhookPayload payload = N8nWebhookPayload.workflowCompleted(clip, video);
        return sendWebhook(payload);
    }

    /**
     * Notifica erros
     */
    @Async
    public CompletableFuture<Boolean> notifyError(String errorType, String errorMessage, Map<String, Object> context) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(false);
        }

        N8nWebhookPayload payload = N8nWebhookPayload.error(errorType, errorMessage, context);
        return sendWebhook(payload);
    }

    /**
     * Envia evento customizado
     */
    @Async
    public CompletableFuture<Boolean> sendCustomEvent(String eventName, Map<String, Object> data) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(false);
        }

        N8nWebhookPayload payload = N8nWebhookPayload.builder()
                .event(eventName)
                .timestamp(LocalDateTime.now())
                .source("AutoClipster")
                .data(data)
                .build();

        return sendWebhook(payload);
    }

    /**
     * Envia webhook para n8n
     */
    private CompletableFuture<Boolean> sendWebhook(N8nWebhookPayload payload) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(false);
        }

        // Verificar rate limit
        if (rateLimitService.isRateLimited("n8n-webhook")) {
            log.warn("Rate limit atingido para webhooks n8n");
            return CompletableFuture.completedFuture(false);
        }

        try {
            // Adquirir permissão para envio
            rateLimitService.acquirePermission("n8n-webhook", 10, Duration.ofMinutes(1));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<N8nWebhookPayload> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Webhook n8n enviado com sucesso: {}", payload.getEvent());
                return CompletableFuture.completedFuture(true);
            } else {
                log.warn("Webhook n8n retornou status não-sucesso: {}", response.getStatusCode());
                return CompletableFuture.completedFuture(false);
            }
            
        } catch (Exception e) {
            log.error("Erro ao enviar webhook n8n: ", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Testa conectividade com n8n
     */
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        if (!isEnabled()) {
            result.put("success", false);
            result.put("message", "Serviço n8n não está habilitado ou configurado");
            return result;
        }

        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("test", true);
            testData.put("timestamp", LocalDateTime.now());
            
            CompletableFuture<Boolean> testResult = sendCustomEvent("test.connection", testData);
            boolean success = testResult.get();
            
            result.put("success", success);
            result.put("message", success ? "Conexão com n8n bem-sucedida" : "Falha na conexão com n8n");
            result.put("webhook_url", webhookUrl);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Erro ao testar conexão: " + e.getMessage());
            log.error("Erro ao testar conexão n8n: ", e);
        }
        
        return result;
    }

    /**
     * Obtém estatísticas do webhook
     */
    public Map<String, Object> getWebhookStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enabled", n8nEnabled);
        stats.put("webhook_url", webhookUrl);
        stats.put("timeout_seconds", timeoutSeconds);
        stats.put("max_retry_attempts", maxRetryAttempts);
        stats.put("retry_delay_seconds", retryDelaySeconds);
        return stats;
    }

    /**
     * Atualiza URL do webhook dinamicamente
     */
    public void setWebhookUrl(String url) {
        this.webhookUrl = url;
        log.info("URL do webhook n8n atualizada: {}", url);
    }

    /**
     * Habilita/desabilita integração n8n
     */
    public void setEnabled(boolean enabled) {
        this.n8nEnabled = enabled;
        log.info("Integração n8n {}", enabled ? "habilitada" : "desabilitada");
    }
} 
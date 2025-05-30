package com.joel.br.AutoClipster.services;

import com.joel.br.AutoClipster.execption.RateLimitExceededException;
import com.joel.br.AutoClipster.limiter.ApiRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço para controle de taxa de requisições (rate limiting)
 * para APIs externas como Twitch, YouTube, TikTok e OpenAI.
 */
@Service
@Slf4j
public class RateLimitService {

    /**
     * Armazena os limitadores para diferentes APIs
     * A chave é o identificador da API
     */
    private final Map<String, ApiRateLimiter> rateLimiters = new ConcurrentHashMap<>();

    /**
     * Adquire uma permissão para executar uma operação em uma API específica
     * Se o limite de taxa for excedido, aguardará ou lançará uma exceção dependendo do timeout
     *
     * @param apiKey Identificador único da API (ex: "twitch-api", "youtube-upload")
     * @param maxRequestsPerInterval Máximo de requisições permitidas no intervalo
     * @param interval Intervalo de tempo para o limite de requisições
     * @param timeout Tempo máximo de espera por uma permissão (opcional)
     */
    public void acquirePermission(String apiKey, int maxRequestsPerInterval, Duration interval, Duration timeout) {
        ApiRateLimiter limiter = rateLimiters.computeIfAbsent(apiKey, 
                k -> new ApiRateLimiter(k, maxRequestsPerInterval, interval));
        
        try {
            boolean acquired;
            
            if (timeout != null) {
                acquired = limiter.tryAcquire(timeout);
            } else {
                limiter.acquire();
                acquired = true;
            }
            
            if (!acquired) {
                throw new RateLimitExceededException("Limite de taxa excedido para " + apiKey);
            }
            
            // Agenda a liberação da permissão após o intervalo dividido pelo número máximo de requests
            long releaseDelayMs = interval.toMillis() / maxRequestsPerInterval;
            schedulePermissionRelease(limiter, releaseDelayMs);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RateLimitExceededException("Interrompido ao aguardar permissão para " + apiKey, e);
        }
    }

    /**
     * Versão simplificada que aguarda indefinidamente por uma permissão
     */
    public void acquirePermission(String apiKey, int maxRequestsPerInterval, Duration interval) {
        acquirePermission(apiKey, maxRequestsPerInterval, interval, null);
    }

    /**
     * Agenda a liberação de uma permissão após um atraso específico
     */
    private void schedulePermissionRelease(ApiRateLimiter limiter, long delayMs) {
        Thread releaseThread = new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                limiter.release();
                log.trace("Permissão liberada para {}. Permissões disponíveis: {}", 
                        limiter.getApiKey(), limiter.getAvailablePermits());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrompido ao liberar permissão para {}", limiter.getApiKey());
            }
        });
        releaseThread.setDaemon(true);
        releaseThread.start();
    }

    /**
     * Verifica se uma API específica está atualmente limitada por rate limit
     * 
     * @param apiKey Identificador da API
     * @return true se não houver permissões disponíveis
     */
    public boolean isRateLimited(String apiKey) {
        ApiRateLimiter limiter = rateLimiters.get(apiKey);
        return limiter != null && limiter.isRateLimited();
    }

    /**
     * Retorna a quantidade de permissões disponíveis para uma API
     * 
     * @param apiKey Identificador da API
     * @return número de permissões disponíveis ou -1 se a API não estiver registrada
     */
    public int getAvailablePermits(String apiKey) {
        ApiRateLimiter limiter = rateLimiters.get(apiKey);
        return limiter != null ? limiter.getAvailablePermits() : -1;
    }
    
    /**
     * Retorna o tempo estimado de espera para uma API específica
     * 
     * @param apiKey Identificador da API
     * @return Duração estimada de espera ou null se a API não estiver registrada
     */
    public Duration getEstimatedWaitTime(String apiKey) {
        ApiRateLimiter limiter = rateLimiters.get(apiKey);
        return limiter != null ? limiter.getEstimatedWaitTime() : null;
    }
    
    /**
     * Obtém relatório de status de todos os limitadores
     * 
     * @return Mapa com informações de status para cada API
     */
    public Map<String, String> getAllLimiterStatus() {
        Map<String, String> status = new HashMap<>();
        rateLimiters.forEach((key, limiter) -> {
            status.put(key, limiter.getStatusReport());
        });
        return status;
    }
}

package com.joel.br.AutoClipster.limiter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementa um limitador de taxa para APIs externas usando o algoritmo de token bucket.
 * Cada instância controla o limite de taxa para uma API específica.
 */
@Slf4j
public class ApiRateLimiter {
    
    @Getter
    private final String apiKey;
    
    @Getter
    private final Semaphore semaphore;
    
    @Getter
    private final int maxPermits;
    
    @Getter
    private final Duration interval;
    
    @Getter
    private volatile LocalDateTime lastReleaseTime;
    
    private final AtomicLong totalRequestsMade = new AtomicLong(0);
    private final LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * Cria um novo limitador de taxa para uma API
     * 
     * @param apiKey Identificador único da API
     * @param maxPermits Número máximo de requisições permitidas no intervalo
     * @param interval Intervalo de tempo para o limite de requisições
     */
    public ApiRateLimiter(String apiKey, int maxPermits, Duration interval) {
        this.apiKey = apiKey;
        this.maxPermits = maxPermits;
        this.interval = interval;
        // Usar um semáforo justo (fair) garante que as threads obtenham permissões na ordem de chegada
        this.semaphore = new Semaphore(maxPermits, true);
        this.lastReleaseTime = LocalDateTime.now();
        
        log.debug("Criado limitador de taxa para {}: {} requisições por {}", 
                apiKey, maxPermits, formatDuration(interval));
    }
    
    /**
     * Tenta adquirir uma permissão, bloqueando até que esteja disponível
     * 
     * @throws InterruptedException se a thread for interrompida enquanto aguarda
     */
    public void acquire() throws InterruptedException {
        semaphore.acquire();
        log.trace("Permissão adquirida para {}. Restantes: {}/{}", 
                apiKey, semaphore.availablePermits(), maxPermits);
    }
    
    /**
     * Tenta adquirir uma permissão, aguardando apenas pelo tempo especificado
     * 
     * @param timeout Tempo máximo de espera
     * @return true se a permissão foi adquirida, false caso contrário
     * @throws InterruptedException se a thread for interrompida enquanto aguarda
     */
    public boolean tryAcquire(Duration timeout) throws InterruptedException {
        boolean acquired = semaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
        
        if (acquired) {
            log.trace("Permissão adquirida para {} após espera. Restantes: {}/{}", 
                    apiKey, semaphore.availablePermits(), maxPermits);
        } else {
            log.debug("Timeout ao aguardar permissão para {}. Limite de taxa excedido.", apiKey);
        }
        
        return acquired;
    }
    
    /**
     * Libera uma permissão, tornando-a disponível para outras threads
     */
    public void release() {
        semaphore.release();
        updateLastReleaseTime();
    }
    
    /**
     * Atualiza o timestamp da última liberação e incrementa o contador total
     */
    public void updateLastReleaseTime() {
        this.lastReleaseTime = LocalDateTime.now();
        this.totalRequestsMade.incrementAndGet();
    }
    
    /**
     * Retorna o número de permissões disponíveis atualmente
     * 
     * @return número de permissões disponíveis
     */
    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }
    
    /**
     * Verifica se o limitador está atualmente saturado (sem permissões disponíveis)
     * 
     * @return true se não houver permissões disponíveis
     */
    public boolean isRateLimited() {
        return semaphore.availablePermits() == 0;
    }
    
    /**
     * Calcula a taxa média de requisições por unidade de tempo
     * 
     * @return taxa de requisições por segundo
     */
    public double getRequestRate() {
        long totalSeconds = Duration.between(createdAt, LocalDateTime.now()).getSeconds();
        if (totalSeconds == 0) return 0;
        return (double) totalRequestsMade.get() / totalSeconds;
    }
    
    /**
     * Calcula o tempo médio entre requisições
     * 
     * @return duração média entre requisições
     */
    public Duration getAverageTimeBetweenRequests() {
        long total = totalRequestsMade.get();
        if (total <= 1) return Duration.ZERO;
        
        long totalMillis = Duration.between(createdAt, LocalDateTime.now()).toMillis();
        return Duration.ofMillis(totalMillis / total);
    }
    
    /**
     * Retorna o tempo estimado de espera para a próxima permissão disponível
     * 
     * @return duração estimada de espera
     */
    public Duration getEstimatedWaitTime() {
        if (semaphore.availablePermits() > 0) return Duration.ZERO;
        
        // Estimamos com base no intervalo total dividido pelo número máximo de requisições
        return interval.dividedBy(maxPermits);
    }
    
    /**
     * Retorna um relatório de status do limitador
     * 
     * @return String contendo estatísticas do limitador
     */
    public String getStatusReport() {
        return String.format(
            "API: %s, Permits: %d/%d, Rate: %.2f req/s, Avg interval: %s, Last release: %s ago",
            apiKey,
            semaphore.availablePermits(),
            maxPermits,
            getRequestRate(),
            formatDuration(getAverageTimeBetweenRequests()),
            formatDuration(Duration.between(lastReleaseTime, LocalDateTime.now()))
        );
    }
    
    /**
     * Formata uma duração para exibição legível
     */
    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        
        if (absSeconds < 60) {
            return seconds + "s";
        }
        
        long minutes = absSeconds / 60;
        if (minutes < 60) {
            return minutes + "m " + (absSeconds % 60) + "s";
        }
        
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h " + (minutes % 60) + "m";
        }
        
        long days = hours / 24;
        return days + "d " + (hours % 24) + "h";
    }
}
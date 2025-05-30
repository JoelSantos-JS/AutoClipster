package com.joel.br.AutoClipster.services;

import com.joel.br.AutoClipster.model.DownloadedClip;
import com.joel.br.AutoClipster.repository.DownloadedClipRepository;
import com.joel.br.AutoClipster.services.GeminiAnalysisService.ClipAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço responsável pelo processamento automático de clips
 * Integra download → análise com Gemini → preparação para upload
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AutomatedClipProcessingService {

    private final DownloadedClipRepository downloadedClipRepository;
    private final GeminiAnalysisService geminiAnalysisService;

    @Value("${automation.quality.min-viral-score:6.0}")
    private Double minViralScore;

    @Value("${automation.quality.min-duration:10}")
    private Integer minDuration;

    @Value("${automation.quality.max-duration:180}")
    private Integer maxDuration;

    @Value("${automation.quality.min-views:100}")
    private Integer minViews;

    @Value("${automation.auto-upload.enabled:false}")
    private Boolean autoUploadEnabled;

    @Value("${automation.auto-upload.min-score:8.0}")
    private Double autoUploadMinScore;

    /**
     * Processa um clip recém-baixado de forma assíncrona
     */
    @Async
    @Transactional
    public CompletableFuture<Void> processNewlyDownloadedClip(DownloadedClip downloadedClip) {
        log.info("🔄 Iniciando processamento automático do clip: {}", downloadedClip.getTitle());
        
        try {
            // 1. Marcar como processando
            downloadedClip.setProcessingStatus("ANALYZING");
            downloadedClipRepository.save(downloadedClip);
            
            // 2. Executar análise completa com Gemini
            ClipAnalysis analysis = performFullGeminiAnalysis(downloadedClip);
            
            // 3. Aplicar filtros de qualidade
            if (!passesQualityFilter(analysis, downloadedClip)) {
                log.info("❌ Clip '{}' não passou no filtro de qualidade", downloadedClip.getTitle());
                downloadedClip.setProcessingStatus("SKIPPED");
                downloadedClip.setProcessed(true);
                downloadedClipRepository.save(downloadedClip);
                return CompletableFuture.completedFuture(null);
            }
            
            // 4. Salvar resultados da análise
            saveAnalysisResults(downloadedClip, analysis);
            
            // 5. Marcar como processado com sucesso
            downloadedClip.setProcessingStatus("READY_FOR_UPLOAD");
            downloadedClip.setProcessed(true);
            downloadedClipRepository.save(downloadedClip);
            
            log.info("✅ Processamento automático concluído para: {} (Score: {})", 
                    downloadedClip.getTitle(), analysis.getViralScore());
            
        } catch (Exception e) {
            log.error("❌ Erro no processamento automático do clip {}: {}", 
                     downloadedClip.getTitle(), e.getMessage());
            downloadedClip.setProcessingStatus("FAILED");
            downloadedClip.setProcessed(true);
            downloadedClipRepository.save(downloadedClip);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Job agendado para processar clips pendentes a cada 5 minutos
     */
    @Scheduled(fixedDelay = 300000) // 5 minutos
    @Transactional
    public void processUnprocessedClips() {
        log.debug("🔍 Verificando clips não processados...");
        
        List<DownloadedClip> unprocessedClips = downloadedClipRepository.findByProcessedFalse();
        
        if (unprocessedClips.isEmpty()) {
            log.debug("✅ Nenhum clip pendente para processamento");
            return;
        }
        
        log.info("📦 Encontrados {} clips para processamento automático", unprocessedClips.size());
        
        for (DownloadedClip clip : unprocessedClips) {
            // Evitar processar clips muito recentes (dar tempo para o download finalizar)
            if (clip.getDownloadDate().isAfter(LocalDateTime.now().minusMinutes(2))) {
                log.debug("⏳ Clip muito recente, aguardando: {}", clip.getTitle());
                continue;
            }
            
            processNewlyDownloadedClip(clip);
        }
    }

    /**
     * Executa análise completa usando todos os recursos do Gemini
     */
    private ClipAnalysis performFullGeminiAnalysis(DownloadedClip clip) {
        log.info("🤖 Executando análise completa com Gemini para: {}", clip.getTitle());
        
        try {
            // Análise principal com Function Calling
            ClipAnalysis analysis = geminiAnalysisService.analyzeClipWithFunctionCalling(
                clip.getTitle(),
                "Clip de gaming do streamer " + clip.getBroadcasterName() + 
                " jogando " + clip.getGameName() + ". Duração: " + clip.getDuration() + "s",
                clip.getBroadcasterName(),
                clip.getGameName()
            );
            
            log.info("✅ Análise básica concluída. Score viral: {}", analysis.getViralScore());
            
            // Enriquecer com análise usando Google Search para trends
            try {
                ClipAnalysis trendAnalysis = geminiAnalysisService.analyzeClipWithGoogleSearch(
                    clip.getTitle(),
                    analysis.getOptimizedDescription(),
                    clip.getBroadcasterName(),
                    clip.getGameName()
                );
                
                // Combinar os melhores resultados
                analysis = mergeBestAnalysisResults(analysis, trendAnalysis);
                log.info("✅ Análise com Google Search integrada");
                
            } catch (Exception e) {
                log.warn("⚠️ Erro na análise com Google Search, usando análise básica: {}", e.getMessage());
            }
            
            return analysis;
            
        } catch (Exception e) {
            log.error("❌ Erro na análise com Gemini: {}", e.getMessage());
            throw new RuntimeException("Falha na análise do clip: " + e.getMessage(), e);
        }
    }

    /**
     * Aplica filtros de qualidade para determinar se o clip deve ser processado
     */
    private boolean passesQualityFilter(ClipAnalysis analysis, DownloadedClip clip) {
        log.info("🔍 Aplicando filtros de qualidade para: {}", clip.getTitle());
        
        // 1. Score viral mínimo
        if (analysis.getViralScore() < minViralScore) {
            log.info("❌ Score viral muito baixo: {} (mínimo: {})", 
                    analysis.getViralScore(), minViralScore);
            return false;
        }
        
        // 2. Duração mínima/máxima
        if (clip.getDuration() < minDuration || clip.getDuration() > maxDuration) {
            log.info("❌ Duração inadequada: {}s (range: {}-{}s)", 
                    clip.getDuration(), minDuration, maxDuration);
            return false;
        }
        
        // 3. View count mínimo (se disponível)
        if (clip.getViewCount() != null && clip.getViewCount() < minViews) {
            log.info("❌ View count muito baixo: {} (mínimo: {})", 
                    clip.getViewCount(), minViews);
            return false;
        }
        
        // 4. Filtro de conteúdo inapropriado
        if (containsInappropriateContent(analysis.getOptimizedTitle()) || 
            containsInappropriateContent(analysis.getOptimizedDescription())) {
            log.info("❌ Conteúdo inapropriado detectado");
            return false;
        }
        
        // 5. Verificar se tem conteúdo mínimo de qualidade
        if (analysis.getTags().size() < 3) {
            log.info("❌ Muito poucas tags geradas: {}", analysis.getTags().size());
            return false;
        }
        
        log.info("✅ Clip passou em todos os filtros de qualidade");
        return true;
    }

    /**
     * Salva os resultados da análise no clip
     */
    private void saveAnalysisResults(DownloadedClip clip, ClipAnalysis analysis) {
        // Criar um JSON com os resultados da análise
        String analysisJson = String.format("""
            {
                "optimizedTitle": "%s",
                "optimizedDescription": "%s",
                "tags": %s,
                "viralScore": %.2f,
                "category": "%s",
                "sentiment": "%s",
                "estimatedViews": %d,
                "bestUploadTime": "%s",
                "socialHashtags": %s,
                "thumbnailSuggestion": "%s"
            }
            """,
            escapeJson(analysis.getOptimizedTitle()),
            escapeJson(analysis.getOptimizedDescription()),
            analysis.getTags().toString(),
            analysis.getViralScore(),
            analysis.getCategory(),
            analysis.getSentiment(),
            analysis.getEstimatedViews(),
            analysis.getBestUploadTime(),
            analysis.getSocialHashtags().toString(),
            escapeJson(analysis.getThumbnailSuggestion())
        );
        
        // Salvar no campo processingStatus como JSON
        clip.setProcessingStatus("ANALYZED: " + analysisJson);
        
        log.info("💾 Resultados da análise salvos para: {}", clip.getTitle());
    }

    /**
     * Combina os melhores resultados de duas análises
     */
    private ClipAnalysis mergeBestAnalysisResults(ClipAnalysis primary, ClipAnalysis secondary) {
        // Usar o melhor título (mais específico)
        String bestTitle = primary.getOptimizedTitle().length() > secondary.getOptimizedTitle().length() 
            ? primary.getOptimizedTitle() : secondary.getOptimizedTitle();
        
        // Usar a melhor descrição (mais detalhada)
        String bestDescription = primary.getOptimizedDescription().length() > secondary.getOptimizedDescription().length()
            ? primary.getOptimizedDescription() : secondary.getOptimizedDescription();
        
        // Combinar tags únicas
        List<String> combinedTags = primary.getTags();
        secondary.getTags().forEach(tag -> {
            if (!combinedTags.contains(tag)) {
                combinedTags.add(tag);
            }
        });
        
        // Usar o maior score viral
        Double bestScore = Math.max(primary.getViralScore(), secondary.getViralScore());
        
        // Usar a maior estimativa de views
        Integer bestViews = Math.max(primary.getEstimatedViews(), secondary.getEstimatedViews());
        
        return ClipAnalysis.builder()
            .optimizedTitle(bestTitle)
            .optimizedDescription(bestDescription)
            .tags(combinedTags)
            .viralScore(bestScore)
            .category(primary.getCategory()) // Manter categoria original
            .sentiment(primary.getSentiment())
            .estimatedViews(bestViews)
            .bestUploadTime(secondary.getBestUploadTime()) // Usar tempo do search (mais atual)
            .socialHashtags(combinedTags.subList(0, Math.min(5, combinedTags.size())))
            .thumbnailSuggestion(primary.getThumbnailSuggestion())
            .build();
    }

    /**
     * Verifica se o conteúdo contém termos inapropriados
     */
    private boolean containsInappropriateContent(String content) {
        if (content == null) return false;
        
        String lowerContent = content.toLowerCase();
        
        // Lista básica de termos a evitar
        List<String> inappropriateTerms = Arrays.asList(
            "hack", "cheat", "exploit", "bug abuse", "toxic", "rage quit"
        );
        
        return inappropriateTerms.stream().anyMatch(lowerContent::contains);
    }

    /**
     * Escapa caracteres especiais para JSON
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * Retorna estatísticas do processamento
     */
    public void logProcessingStatistics() {
        long total = downloadedClipRepository.count();
        long processed = downloadedClipRepository.countByProcessedTrue();
        long pending = downloadedClipRepository.countByProcessedFalse();
        
        log.info("📊 Estatísticas de Processamento:");
        log.info("  ├── Total de clips: {}", total);
        log.info("  ├── Processados: {}", processed);
        log.info("  ├── Pendentes: {}", pending);
        log.info("  └── Taxa de sucesso: {:.1f}%", 
                total > 0 ? (processed * 100.0 / total) : 0);
    }
} 
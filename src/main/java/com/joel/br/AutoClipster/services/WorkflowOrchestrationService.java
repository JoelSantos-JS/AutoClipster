package com.joel.br.AutoClipster.services;

import com.joel.br.AutoClipster.DTO.TwitchClipDTO;
import com.joel.br.AutoClipster.DTO.TwitchUserDTO;
import com.joel.br.AutoClipster.DTO.WorkflowResult;
import com.joel.br.AutoClipster.repository.DownloadedClipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Orquestra o workflow completo de processamento de clips
 * Canal → Buscar Clips → Download → Análise Automática
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowOrchestrationService {

    private final TwitchService twitchService;
    private final ClipDownloadService clipDownloadService;
    private final AutomatedClipProcessingService processingService;
    private final DownloadedClipRepository downloadedClipRepository;

    /**
     * Executa o workflow completo de forma assíncrona
     */
    @Async
    public CompletableFuture<WorkflowResult> executeCompleteWorkflow(
            String channelName, 
            int clipLimit, 
            int daysBack) {
        
        LocalDateTime startTime = LocalDateTime.now();
        log.info("🚀 Iniciando workflow completo para canal: {} ({} clips, {} dias)", 
                channelName, clipLimit, daysBack);
        
        try {
            // 1. BUSCAR CANAL
            log.info("🔍 Buscando canal: {}", channelName);
            TwitchUserDTO user = twitchService.getUserByName(channelName).block();
            
            if (user == null) {
                return CompletableFuture.completedFuture(
                    WorkflowResult.builder()
                        .channelName(channelName)
                        .status("FAILED")
                        .errorMessage("Canal não encontrado: " + channelName)
                        .startedAt(startTime)
                        .completedAt(LocalDateTime.now())
                        .build()
                );
            }
            
            log.info("✅ Canal encontrado: {} (ID: {})", user.getDisplayName(), user.getId());
            
            // 2. BUSCAR CLIPS DO CANAL
            log.info("🎬 Buscando clips dos últimos {} dias...", daysBack);
            Flux<TwitchClipDTO> clipsFlux = twitchService.getClipsFromChannelExtended(user.getId(), daysBack);
            
            // 3. DOWNLOAD AUTOMÁTICO DOS MELHORES CLIPS
            log.info("📥 Iniciando download dos {} melhores clips...", clipLimit);
            Integer downloadedCount = clipDownloadService.downloadTopClips(clipsFlux, clipLimit).block();
            
            if (downloadedCount == null) {
                downloadedCount = 0;
            }
            
            log.info("✅ Download concluído: {} clips baixados", downloadedCount);
            
            // 4. AGUARDAR UM POUCO PARA O PROCESSAMENTO AUTOMÁTICO INICIAR
            if (downloadedCount > 0) {
                log.info("⏳ Aguardando início do processamento automático...");
                Thread.sleep(5000); // 5 segundos
            }
            
            // 5. COLETAR ESTATÍSTICAS
            long totalProcessed = downloadedClipRepository.countByProcessedTrue();
            
            // 6. PREPARAR RESULTADO
            WorkflowResult result = WorkflowResult.builder()
                .channelName(channelName)
                .channelId(user.getId())
                .clipsDownloaded(downloadedCount)
                .clipsProcessed(Math.toIntExact(totalProcessed))
                .status("COMPLETED")
                .startedAt(startTime)
                .completedAt(LocalDateTime.now())
                .processedClipTitles(new ArrayList<>()) // Poderia buscar os títulos se necessário
                .build();
            
            log.info("🎉 Workflow concluído com sucesso para canal: {}", channelName);
            logWorkflowSummary(result);
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            log.error("❌ Erro no workflow para canal {}: {}", channelName, e.getMessage());
            
            return CompletableFuture.completedFuture(
                WorkflowResult.builder()
                    .channelName(channelName)
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .startedAt(startTime)
                    .completedAt(LocalDateTime.now())
                    .clipsDownloaded(0)
                    .clipsProcessed(0)
                    .build()
            );
        }
    }

    /**
     * Executa workflow para múltiplos canais
     */
    @Async
    public CompletableFuture<List<WorkflowResult>> executeMultipleChannelsWorkflow(
            List<String> channelNames, 
            int clipLimitPerChannel, 
            int daysBack) {
        
        log.info("🚀 Iniciando workflow para múltiplos canais: {} canais", channelNames.size());
        
        List<CompletableFuture<WorkflowResult>> futures = new ArrayList<>();
        
        for (String channelName : channelNames) {
            CompletableFuture<WorkflowResult> future = executeCompleteWorkflow(
                channelName, clipLimitPerChannel, daysBack);
            futures.add(future);
            
            try {
                // Delay entre canais para evitar rate limiting
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.warn("Interrompido durante delay entre canais");
            }
        }
        
        // Aguardar todos os workflows
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    /**
     * Verifica o status atual de todos os workflows
     */
    public WorkflowResult getWorkflowStatus() {
        long totalClips = downloadedClipRepository.count();
        long processedClips = downloadedClipRepository.countByProcessedTrue();
        long pendingClips = downloadedClipRepository.countByProcessedFalse();
        
        return WorkflowResult.builder()
            .status(pendingClips > 0 ? "IN_PROGRESS" : "COMPLETED")
            .clipsDownloaded(Math.toIntExact(totalClips))
            .clipsProcessed(Math.toIntExact(processedClips))
            .completedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Força o reprocessamento de clips que falharam
     */
    @Async
    public CompletableFuture<Integer> retryFailedClips() {
        log.info("🔄 Iniciando reprocessamento de clips que falharam...");
        
        List<com.joel.br.AutoClipster.model.DownloadedClip> failedClips = 
            downloadedClipRepository.findAll().stream()
                .filter(clip -> clip.getProcessingStatus() != null && 
                               clip.getProcessingStatus().equals("FAILED"))
                .toList();
        
        log.info("📦 Encontrados {} clips que falharam no processamento", failedClips.size());
        
        int retriedCount = 0;
        for (var clip : failedClips) {
            try {
                clip.setProcessed(false);
                clip.setProcessingStatus("RETRY");
                downloadedClipRepository.save(clip);
                
                processingService.processNewlyDownloadedClip(clip);
                retriedCount++;
                
                log.info("🔄 Reprocessamento iniciado para: {}", clip.getTitle());
                
            } catch (Exception e) {
                log.error("❌ Erro ao reprocessar clip {}: {}", clip.getTitle(), e.getMessage());
            }
        }
        
        log.info("✅ Reprocessamento iniciado para {} clips", retriedCount);
        return CompletableFuture.completedFuture(retriedCount);
    }

    /**
     * Log do resumo do workflow
     */
    private void logWorkflowSummary(WorkflowResult result) {
        log.info("📊 Resumo do Workflow:");
        log.info("  ├── Canal: {}", result.getChannelName());
        log.info("  ├── Clips baixados: {}", result.getClipsDownloaded());
        log.info("  ├── Clips processados: {}", result.getClipsProcessed());
        log.info("  ├── Status: {}", result.getStatus());
        log.info("  ├── Início: {}", result.getStartedAt());
        log.info("  └── Conclusão: {}", result.getCompletedAt());
    }

    /**
     * Limpa clips antigos baseado em configuração
     */
    public void cleanupOldClips(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        
        List<com.joel.br.AutoClipster.model.DownloadedClip> oldClips = 
            downloadedClipRepository.findAll().stream()
                .filter(clip -> clip.getDownloadDate().isBefore(cutoffDate))
                .toList();
        
        if (!oldClips.isEmpty()) {
            log.info("🧹 Limpando {} clips antigos (mais de {} dias)", oldClips.size(), daysToKeep);
            downloadedClipRepository.deleteAll(oldClips);
        }
    }
} 
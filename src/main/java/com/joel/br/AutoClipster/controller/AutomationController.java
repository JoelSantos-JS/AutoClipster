package com.joel.br.AutoClipster.controller;

import com.joel.br.AutoClipster.DTO.AutomationStatus;
import com.joel.br.AutoClipster.DTO.WorkflowResult;
import com.joel.br.AutoClipster.services.WorkflowOrchestrationService;
import com.joel.br.AutoClipster.services.AutomatedClipProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller para gerenciar a automação completa do sistema
 * Integra download, análise e processamento automático
 */
@RestController
@RequestMapping("/api/automation")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AutomationController {

    private final WorkflowOrchestrationService workflowService;
    private final AutomatedClipProcessingService processingService;

    /**
     * Executa o workflow completo para um canal
     * 🚀 Canal → Clips → Download → Análise Automática
     */
    @PostMapping("/execute")
    public CompletableFuture<ResponseEntity<WorkflowResult>> executeCompleteWorkflow(
            @RequestBody WorkflowRequest request) {
        
        log.info("🚀 Executando workflow automático para canal: {}", request.getChannelName());
        
        return workflowService.executeCompleteWorkflow(
                request.getChannelName(),
                request.getClipLimit(),
                request.getDaysBack())
            .thenApply(result -> {
                log.info("✅ Workflow concluído para canal: {} - Status: {}", 
                        request.getChannelName(), result.getStatus());
                return ResponseEntity.ok(result);
            })
            .exceptionally(ex -> {
                log.error("❌ Erro no workflow para canal {}: {}", request.getChannelName(), ex.getMessage());
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Executa workflow para múltiplos canais
     */
    @PostMapping("/execute-multiple")
    public CompletableFuture<ResponseEntity<List<WorkflowResult>>> executeMultipleChannelsWorkflow(
            @RequestBody MultipleChannelsRequest request) {
        
        log.info("🚀 Executando workflow para {} canais", request.getChannelNames().size());
        
        return workflowService.executeMultipleChannelsWorkflow(
                request.getChannelNames(),
                request.getClipLimitPerChannel(),
                request.getDaysBack())
            .thenApply(results -> {
                long successCount = results.stream()
                    .mapToLong(r -> "COMPLETED".equals(r.getStatus()) ? 1 : 0)
                    .sum();
                
                log.info("✅ Workflow múltiplo concluído: {}/{} canais processados com sucesso", 
                        successCount, results.size());
                return ResponseEntity.ok(results);
            })
            .exceptionally(ex -> {
                log.error("❌ Erro no workflow múltiplo: {}", ex.getMessage());
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Verifica o status atual da automação
     */
    @GetMapping("/status")
    public ResponseEntity<AutomationStatus> getAutomationStatus() {
        try {
            log.info("📊 Consultando status da automação");
            
            WorkflowResult workflowStatus = workflowService.getWorkflowStatus();
            
            AutomationStatus status = AutomationStatus.builder()
                .totalClipsDownloaded(workflowStatus.getClipsDownloaded() != null ? workflowStatus.getClipsDownloaded().longValue() : 0L)
                .totalClipsProcessed(workflowStatus.getClipsProcessed() != null ? workflowStatus.getClipsProcessed().longValue() : 0L)
                .isProcessingActive(!workflowStatus.getStatus().equals("COMPLETED"))
                .lastProcessedAt(workflowStatus.getCompletedAt())
                .totalClipsPending(workflowStatus.getClipsDownloaded() != null && workflowStatus.getClipsProcessed() != null ? 
                    (long)(workflowStatus.getClipsDownloaded() - workflowStatus.getClipsProcessed()) : 0L)
                .averageProcessingTime(workflowStatus.getClipsDownloaded() != null && workflowStatus.getClipsDownloaded() > 0 ? 
                    (double) workflowStatus.getClipsProcessed() / workflowStatus.getClipsDownloaded() * 100 : 0.0)
                .build();
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("❌ Erro ao consultar status da automação: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Força o reprocessamento de clips que falharam
     */
    @PostMapping("/retry-failed")
    public CompletableFuture<ResponseEntity<RetryResult>> retryFailedClips() {
        log.info("🔄 Iniciando reprocessamento de clips que falharam");
        
        return workflowService.retryFailedClips()
            .thenApply(count -> {
                RetryResult result = new RetryResult(count, "Reprocessamento iniciado para " + count + " clips");
                log.info("✅ Reprocessamento iniciado para {} clips", count);
                return ResponseEntity.ok(result);
            })
            .exceptionally(ex -> {
                log.error("❌ Erro no reprocessamento: {}", ex.getMessage());
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Execução de limpeza de clips antigos
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<String> cleanupOldClips(@RequestParam(defaultValue = "30") int daysToKeep) {
        try {
            log.info("🧹 Executando limpeza de clips antigos (> {} dias)", daysToKeep);
            workflowService.cleanupOldClips(daysToKeep);
            return ResponseEntity.ok("Limpeza executada com sucesso");
        } catch (Exception e) {
            log.error("❌ Erro na limpeza: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Erro na limpeza: " + e.getMessage());
        }
    }

    /**
     * Webhook para testar integração (modo de desenvolvimento)
     */
    @PostMapping("/test")
    public ResponseEntity<String> testAutomation() {
        log.info("🧪 Testando automação completa");
        
        try {
            // Teste rápido com configurações mínimas
            CompletableFuture<WorkflowResult> testResult = workflowService.executeCompleteWorkflow(
                "gaules", 2, 1);
            
            return ResponseEntity.ok("Teste de automação iniciado para canal 'gaules' (2 clips, 1 dia)");
            
        } catch (Exception e) {
            log.error("❌ Erro no teste de automação: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Erro no teste: " + e.getMessage());
        }
    }

    // DTOs para Request/Response
    
    public static class WorkflowRequest {
        private String channelName;
        private int clipLimit = 10;
        private int daysBack = 7;

        // Getters and Setters
        public String getChannelName() { return channelName; }
        public void setChannelName(String channelName) { this.channelName = channelName; }
        
        public int getClipLimit() { return clipLimit; }
        public void setClipLimit(int clipLimit) { this.clipLimit = clipLimit; }
        
        public int getDaysBack() { return daysBack; }
        public void setDaysBack(int daysBack) { this.daysBack = daysBack; }
    }

    public static class MultipleChannelsRequest {
        private List<String> channelNames;
        private int clipLimitPerChannel = 5;
        private int daysBack = 7;

        // Getters and Setters
        public List<String> getChannelNames() { return channelNames; }
        public void setChannelNames(List<String> channelNames) { this.channelNames = channelNames; }
        
        public int getClipLimitPerChannel() { return clipLimitPerChannel; }
        public void setClipLimitPerChannel(int clipLimitPerChannel) { this.clipLimitPerChannel = clipLimitPerChannel; }
        
        public int getDaysBack() { return daysBack; }
        public void setDaysBack(int daysBack) { this.daysBack = daysBack; }
    }

    public static class RetryResult {
        private final int retriedCount;
        private final String message;

        public RetryResult(int retriedCount, String message) {
            this.retriedCount = retriedCount;
            this.message = message;
        }

        public int getRetriedCount() { return retriedCount; }
        public String getMessage() { return message; }
    }
} 
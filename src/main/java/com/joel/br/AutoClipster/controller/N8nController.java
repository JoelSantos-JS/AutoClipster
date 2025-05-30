package com.joel.br.AutoClipster.controller;

import com.joel.br.AutoClipster.services.N8nWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/n8n")
@RequiredArgsConstructor
@Slf4j
public class N8nController {

    private final N8nWebhookService webhookService;

    /**
     * Testa conectividade com n8n
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        try {
            log.info("Testando conectividade com n8n");
            
            Map<String, Object> result = webhookService.testConnection();
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Erro no teste de conectividade n8n: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Envia evento personalizado para n8n
     */
    @PostMapping("/send-event")
    public ResponseEntity<Map<String, Object>> sendCustomEvent(
            @RequestParam String eventName,
            @RequestBody Map<String, Object> data) {
        
        try {
            log.info("Enviando evento personalizado para n8n: {}", eventName);
            
            webhookService.sendCustomEvent(eventName, data)
                    .thenAccept(success -> {
                        if (success) {
                            log.info("Evento {} enviado com sucesso", eventName);
                        } else {
                            log.warn("Falha ao enviar evento {}", eventName);
                        }
                    });
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Evento enviado para n8n (processamento assíncrono)");
            response.put("eventName", eventName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao enviar evento personalizado: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Configura URL do webhook n8n
     */
    @PostMapping("/config/webhook-url")
    public ResponseEntity<Map<String, Object>> configureWebhookUrl(@RequestParam String url) {
        try {
            log.info("Configurando URL do webhook n8n: {}", url);
            
            webhookService.setWebhookUrl(url);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "URL do webhook configurada com sucesso");
            response.put("webhookUrl", url);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao configurar URL do webhook: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Habilita/desabilita integração n8n
     */
    @PostMapping("/config/enabled")
    public ResponseEntity<Map<String, Object>> setEnabled(@RequestParam boolean enabled) {
        try {
            log.info("{} integração n8n", enabled ? "Habilitando" : "Desabilitando");
            
            webhookService.setEnabled(enabled);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Integração n8n " + (enabled ? "habilitada" : "desabilitada"));
            response.put("enabled", enabled);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao configurar estado do n8n: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Obtém estatísticas e configurações do n8n
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = webhookService.getWebhookStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Erro ao obter estatísticas n8n: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Endpoint para receber callbacks do n8n (se necessário)
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> receiveCallback(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Callback recebido do n8n: {}", payload);
            
            // Aqui você pode processar callbacks do n8n se necessário
            // Por exemplo, quando um workflow n8n completa uma ação
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Callback processado com sucesso");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao processar callback n8n: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Envia notificação de erro para n8n
     */
    @PostMapping("/notify-error")
    public ResponseEntity<Map<String, Object>> notifyError(
            @RequestParam String errorType,
            @RequestParam String errorMessage,
            @RequestBody(required = false) Map<String, Object> context) {
        
        try {
            log.info("Enviando notificação de erro para n8n: {} - {}", errorType, errorMessage);
            
            if (context == null) {
                context = new HashMap<>();
            }
            
            webhookService.notifyError(errorType, errorMessage, context)
                    .thenAccept(success -> {
                        if (success) {
                            log.info("Notificação de erro enviada com sucesso");
                        } else {
                            log.warn("Falha ao enviar notificação de erro");
                        }
                    });
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notificação de erro enviada para n8n");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de erro: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Status geral da integração n8n
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            Map<String, Object> stats = webhookService.getWebhookStats();
            
            status.put("configured", stats.get("webhookUrl") != null && 
                                   !stats.get("webhookUrl").equals("não configurado"));
            status.put("enabled", stats.get("enabled"));
            status.put("webhookUrl", stats.get("webhookUrl"));
            status.put("lastCheck", java.time.LocalDateTime.now());
            
            Map<String, Object> connectionResult = webhookService.testConnection();
            boolean connectivity = (Boolean) connectionResult.getOrDefault("success", false);
            status.put("connectivity", connectivity);
            status.put("status", connectivity ? "ONLINE" : "OFFLINE");
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Erro ao verificar status n8n: ", e);
            
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
            
            return ResponseEntity.ok(status);
        }
    }
} 
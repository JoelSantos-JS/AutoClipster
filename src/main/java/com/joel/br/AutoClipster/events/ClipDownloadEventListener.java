package com.joel.br.AutoClipster.events;

import com.joel.br.AutoClipster.services.AutomatedClipProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Escuta eventos de download de clips e dispara processamento automático
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClipDownloadEventListener {

    private final AutomatedClipProcessingService processingService;

    /**
     * Executa quando um clip é baixado com sucesso
     * Dispara o processamento automático de forma assíncrona
     */
    @EventListener
    @Async
    public void onClipDownloaded(ClipDownloadedEvent event) {
        log.info("🎬 Evento de clip baixado recebido: {} (Source: {})", 
                event.getDownloadedClip().getTitle(), event.getSource());
        
        try {
            // Pequeno delay para garantir que o download finalizou completamente
            Thread.sleep(1000);
            
            // Iniciar processamento automático
            processingService.processNewlyDownloadedClip(event.getDownloadedClip());
            
            log.info("🚀 Processamento automático iniciado para: {}", 
                    event.getDownloadedClip().getTitle());
            
        } catch (Exception e) {
            log.error("❌ Erro ao processar evento de download para clip {}: {}", 
                     event.getDownloadedClip().getTitle(), e.getMessage());
        }
    }
} 
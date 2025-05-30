package com.joel.br.AutoClipster.controller;

import com.joel.br.AutoClipster.DTO.TwitchClipDTO;
import com.joel.br.AutoClipster.DTO.TwitchUserDTO;
import com.joel.br.AutoClipster.model.DownloadedClip;
import com.joel.br.AutoClipster.services.ClipDownloadService;
import com.joel.br.AutoClipster.services.TwitchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/clips")
@RequiredArgsConstructor
@Slf4j
public class ClipController {

    private final ClipDownloadService clipDownloadService;
    private final TwitchService twitchService;
    
    /**
     * Endpoint para baixar um clip específico via URL
     */
    @PostMapping("/download-url")
    public ResponseEntity<String> downloadClipByUrl(@RequestParam String clipUrl) {
        // Extrai o ID do clip da URL (implementação simplificada)
        String clipId = extractClipIdFromUrl(clipUrl);
        if (clipId == null) {
            return ResponseEntity.badRequest().body("URL de clip inválida");
        }
        
        twitchService.getClipById(clipId)
            .subscribe(
                clip -> {
                    try {
                        clipDownloadService.downloadClip(clip);
                        log.info("Clip baixado com sucesso: {}", clip.getTitle());
                    } catch (Exception e) {
                        log.error("Erro ao baixar clip: {}", e.getMessage());
                    }
                },
                error -> log.error("Erro ao buscar clip: {}", error.getMessage())
            );
            
        return ResponseEntity.ok("Download iniciado para " + clipUrl);
    }
    
    /**
     * Endpoint para baixar os N clips mais vistos de um canal
     */
    @GetMapping("/download-top/{channelId}")
    public Mono<ResponseEntity<String>> downloadTopClips(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(7);
        }
        
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        log.info("Baixando os {} clips mais vistos do canal {} entre {} e {}", 
                limit, channelId, startDate, endDate);
                
        return twitchService.getClipsFromChannel(channelId, startDate, endDate)
                .collectList()
                .flatMapMany(clips -> clipDownloadService.downloadTopClips(Flux.fromIterable(clips), limit))
                .next()
                .map(downloadedCount -> ResponseEntity.ok("Download concluído! " + downloadedCount + " clips baixados"))
                .doOnSuccess(response -> log.info("Download concluído"))
                .doOnError(error -> log.error("Erro ao baixar clips: {}", error.getMessage()));
    }
    
    /**
     * Endpoint para baixar clips usando nome do canal (mais user-friendly)
     */
    @GetMapping("/download-top-extended/{channelId}")
    public ResponseEntity<String> downloadTopClipsExtended(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "7") int days) {
        
        log.info("Solicitação de download dos top {} clips do canal {} dos últimos {} dias", 
                limit, channelId, days);
        
        try {
            Flux<TwitchClipDTO> clipsFlux = twitchService.getClipsFromChannelExtended(channelId, days);
            
            Mono<Integer> downloadResult = clipDownloadService.downloadTopClips(clipsFlux, limit);
            
            Integer downloadedCount = downloadResult.block();
            
            if (downloadedCount != null && downloadedCount > 0) {
                log.info("Download concluído com sucesso: {} clips baixados", downloadedCount);
                return ResponseEntity.ok(String.format("Download concluído com sucesso! %d clips baixados do canal %s", 
                        downloadedCount, channelId));
            } else {
                log.warn("Nenhum clip foi baixado para o canal {}", channelId);
                return ResponseEntity.ok("Nenhum clip foi baixado. Verifique se o canal possui clips válidos no período especificado.");
            }
            
        } catch (Exception e) {
            log.error("Erro durante o download dos clips do canal {}: {}", channelId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro durante o download: " + e.getMessage());
        }
    }
    
    /**
     * Endpoint para baixar clips usando nome do canal (mais user-friendly)
     */
    @GetMapping("/download-by-name/{channelName}")
    public ResponseEntity<String> downloadTopClipsByChannelName(
            @PathVariable String channelName,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "7") int days) {
        
        log.info("Buscando clips do canal '{}' para download", channelName);
        
        try {
            // Primeiro, buscar o usuário pelo nome
            Mono<TwitchUserDTO> userMono = twitchService.getUserByName(channelName);
            TwitchUserDTO user = userMono.block();
            
            if (user == null) {
                log.warn("Canal '{}' não encontrado", channelName);
                return ResponseEntity.badRequest()
                        .body("Canal '" + channelName + "' não encontrado. Verifique se o nome está correto.");
            }
            
            log.info("Canal encontrado: {} (ID: {})", user.getDisplayName(), user.getId());
            
            // Buscar clips usando o ID do canal
            Flux<TwitchClipDTO> clipsFlux = twitchService.getClipsFromChannelExtended(user.getId(), days);
            
            Mono<Integer> downloadResult = clipDownloadService.downloadTopClips(clipsFlux, limit);
            
            Integer downloadedCount = downloadResult.block();
            
            if (downloadedCount != null && downloadedCount > 0) {
                log.info("Download concluído com sucesso: {} clips baixados do canal {}", 
                        downloadedCount, user.getDisplayName());
                return ResponseEntity.ok(String.format(
                        "Download concluído com sucesso! %d clips baixados do canal '%s' (%s)", 
                        downloadedCount, user.getDisplayName(), channelName));
            } else {
                log.warn("Nenhum clip foi baixado para o canal {}", channelName);
                return ResponseEntity.ok(String.format(
                        "Nenhum clip foi baixado do canal '%s'. " +
                        "Verifique se o canal possui clips válidos nos últimos %d dias.", 
                        user.getDisplayName(), days));
            }
            
        } catch (Exception e) {
            log.error("Erro durante o download dos clips do canal '{}': {}", channelName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro durante o download: " + e.getMessage());
        }
    }
    
    /**
     * Método auxiliar para extrair o ID do clip a partir da URL
     */
    private String extractClipIdFromUrl(String url) {
        // Implementação simplificada - você precisa adaptar para os formatos reais de URL da Twitch
        // Exemplo de URL: https://clips.twitch.tv/ElegantTenaciousBisonPeteZaroll-XC-gGhgFN_JSlcrp
        
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // Pegar a última parte da URL após a última barra
        String[] parts = url.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        
        return null;
    }
} 
package com.joel.br.AutoClipster.controller;

import com.joel.br.AutoClipster.services.GeminiAnalysisService;
import com.joel.br.AutoClipster.services.GeminiAnalysisService.ClipAnalysis;
import com.joel.br.AutoClipster.services.GeminiAnalysisService.ClipSentiment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller para testar funcionalidades do Gemini AI
 */
@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
@Slf4j
public class GeminiController {

    private final GeminiAnalysisService geminiAnalysisService;

    /**
     * Análise completa de um clip
     * POST /api/gemini/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<ClipAnalysis> analyzeClip(@RequestBody ClipAnalysisRequest request) {
        try {
            log.info("📝 Recebida solicitação de análise para: {}", request.clipTitle);
            
            ClipAnalysis analysis = geminiAnalysisService.analyzeClip(
                request.clipTitle,
                request.clipDescription,
                request.streamerName,
                request.gameName
            );

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            log.error("❌ Erro na análise: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Análise assíncrona de um clip
     * POST /api/gemini/analyze-async
     */
    @PostMapping("/analyze-async")
    public CompletableFuture<ResponseEntity<ClipAnalysis>> analyzeClipAsync(@RequestBody ClipAnalysisRequest request) {
        log.info("🔄 Iniciando análise assíncrona para: {}", request.clipTitle);
        
        return geminiAnalysisService.analyzeClipAsync(
            request.clipTitle,
            request.clipDescription,
            request.streamerName,
            request.gameName
        ).thenApply(ResponseEntity::ok)
         .exceptionally(ex -> {
             log.error("❌ Erro na análise assíncrona: {}", ex.getMessage());
             return ResponseEntity.internalServerError().build();
         });
    }

    /**
     * Gera apenas um título otimizado
     * POST /api/gemini/title
     */
    @PostMapping("/title")
    public ResponseEntity<Map<String, String>> generateTitle(@RequestBody TitleRequest request) {
        try {
            log.info("✏️ Gerando título para: {}", request.originalTitle);
            
            String optimizedTitle = geminiAnalysisService.generateOptimizedTitle(
                request.originalTitle,
                request.streamerName,
                request.gameName
            );

            return ResponseEntity.ok(Map.of(
                "original", request.originalTitle,
                "optimized", optimizedTitle
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao gerar título: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Gera tags para o clip
     * POST /api/gemini/tags
     */
    @PostMapping("/tags")
    public ResponseEntity<Map<String, List<String>>> generateTags(@RequestBody TagsRequest request) {
        try {
            log.info("🏷️ Gerando tags para: {}", request.clipTitle);
            
            List<String> tags = geminiAnalysisService.generateTags(
                request.clipTitle,
                request.streamerName,
                request.gameName
            );

            return ResponseEntity.ok(Map.of("tags", tags));

        } catch (Exception e) {
            log.error("❌ Erro ao gerar tags: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Análise de sentimento
     * POST /api/gemini/sentiment
     */
    @PostMapping("/sentiment")
    public ResponseEntity<Map<String, String>> analyzeSentiment(@RequestBody SentimentRequest request) {
        try {
            log.info("🎭 Analisando sentimento para: {}", request.clipTitle);
            
            ClipSentiment sentiment = geminiAnalysisService.analyzeSentiment(
                request.clipTitle,
                request.clipDescription
            );

            return ResponseEntity.ok(Map.of(
                "sentiment", sentiment.toString(),
                "description", getSentimentDescription(sentiment)
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao analisar sentimento: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Análise com Function Calling
     * POST /api/gemini/analyze-advanced
     */
    @PostMapping("/analyze-advanced")
    public ResponseEntity<ClipAnalysis> analyzeClipWithFunctionCalling(@RequestBody ClipAnalysisRequest request) {
        try {
            log.info("🔧 Recebida solicitação de análise avançada para: {}", request.clipTitle);
            
            ClipAnalysis analysis = geminiAnalysisService.analyzeClipWithFunctionCalling(
                request.clipTitle,
                request.clipDescription,
                request.streamerName,
                request.gameName
            );

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            log.error("❌ Erro na análise avançada: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint de teste simples
     * GET /api/gemini/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testGemini() {
        try {
            log.info("🧪 Testando integração Gemini...");
            
            String testTitle = geminiAnalysisService.generateOptimizedTitle(
                "Streamer faz jogada incrível",
                "TestStreamer",
                "TestGame"
            );

            return ResponseEntity.ok(Map.of(
                "status", "✅ Gemini funcionando",
                "test_result", testTitle,
                "timestamp", java.time.Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("❌ Erro no teste: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "status", "❌ Erro no Gemini",
                "error", e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            ));
        }
    }

    /**
     * Análise streaming de clip
     * POST /api/gemini/analyze-stream
     */
    @PostMapping("/analyze-stream")
    public ResponseEntity<SseEmitter> analyzeClipStream(@RequestBody ClipAnalysisRequest request) {
        try {
            log.info("🌊 Recebida solicitação de análise streaming para: {}", request.clipTitle);
            
            SseEmitter emitter = new SseEmitter(60000L); // 60 segundos timeout

            // Executar análise em thread separada
            CompletableFuture.runAsync(() -> {
                try {
                    geminiAnalysisService.analyzeClipStream(
                        request.clipTitle,
                        request.clipDescription,
                        request.streamerName,
                        request.gameName,
                        (partialResponse) -> {
                            try {
                                emitter.send(SseEmitter.event()
                                    .name("partial-response")
                                    .data(partialResponse));
                            } catch (Exception e) {
                                log.error("❌ Erro ao enviar partial response: {}", e.getMessage());
                            }
                        }
                    );
                    emitter.complete();
                } catch (Exception e) {
                    log.error("❌ Erro na análise streaming: {}", e.getMessage());
                    emitter.completeWithError(e);
                }
            });

            return ResponseEntity.ok(emitter);

        } catch (Exception e) {
            log.error("❌ Erro ao iniciar análise streaming: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Análise multimodal com thumbnail
     * POST /api/gemini/analyze-multimodal
     */
    @PostMapping("/analyze-multimodal")
    public ResponseEntity<ClipAnalysis> analyzeClipWithThumbnail(@RequestBody MultimodalAnalysisRequest request) {
        try {
            log.info("🖼️ Recebida solicitação de análise multimodal para: {}", request.clipTitle);
            
            ClipAnalysis analysis = geminiAnalysisService.analyzeClipWithThumbnail(
                request.clipTitle,
                request.clipDescription,
                request.streamerName,
                request.gameName,
                request.thumbnailUri,
                request.mimeType
            );

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            log.error("❌ Erro na análise multimodal: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Análise com Google Search e Safety Settings
     * POST /api/gemini/analyze-search
     */
    @PostMapping("/analyze-search")
    public ResponseEntity<ClipAnalysis> analyzeClipWithGoogleSearch(@RequestBody ClipAnalysisRequest request) {
        try {
            log.info("🌐 Recebida solicitação de análise com Google Search para: {}", request.clipTitle);
            
            ClipAnalysis analysis = geminiAnalysisService.analyzeClipWithGoogleSearch(
                request.clipTitle,
                request.clipDescription,
                request.streamerName,
                request.gameName
            );

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            log.error("❌ Erro na análise com Google Search: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getSentimentDescription(ClipSentiment sentiment) {
        return switch (sentiment) {
            case POSITIVE -> "Clip positivo - engraçado, impressionante ou emocionante";
            case NEGATIVE -> "Clip negativo - frustrante, triste ou com raiva";
            case NEUTRAL -> "Clip neutro - informativo ou normal";
        };
    }

    // DTOs para as requisições

    public static class ClipAnalysisRequest {
        public String clipTitle;
        public String clipDescription;
        public String streamerName;
        public String gameName;
    }

    public static class MultimodalAnalysisRequest {
        public String clipTitle;
        public String clipDescription;
        public String streamerName;
        public String gameName;
        public String thumbnailUri;
        public String mimeType;
    }

    public static class TitleRequest {
        public String originalTitle;
        public String streamerName;
        public String gameName;
    }

    public static class TagsRequest {
        public String clipTitle;
        public String streamerName;
        public String gameName;
    }

    public static class SentimentRequest {
        public String clipTitle;
        public String clipDescription;
    }
} 
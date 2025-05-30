package com.joel.br.AutoClipster.services;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.joel.br.AutoClipster.config.GeminiSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço para análise de clips usando Google Gemini AI
 * Gera títulos, descrições e tags automaticamente
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiAnalysisService {

    private final Client geminiClient;
    private final GeminiSettings geminiSettings;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Analisa um clip e gera título, descrição e tags
     */
    public ClipAnalysis analyzeClip(String clipTitle, String clipDescription, String streamerName, String gameName) {
        try {
            log.info("🔍 Analisando clip: {}", clipTitle);

            String prompt = buildAnalysisPrompt(clipTitle, clipDescription, streamerName, gameName);
            
            // Configuração com schema JSON para garantir resposta estruturada
            Schema responseSchema = Schema.builder()
                .type("object")
                .properties(ImmutableMap.of(
                    "title", Schema.builder().type(Type.Known.STRING).description("Título otimizado").build(),
                    "description", Schema.builder().type(Type.Known.STRING).description("Descrição detalhada").build(),
                    "tags", Schema.builder()
                        .type("array")
                        .items(Schema.builder().type(Type.Known.STRING).build())
                        .description("Lista de tags").build(),
                    "category", Schema.builder().type(Type.Known.STRING).description("Categoria do clip").build(),
                    "thumbnail_suggestion", Schema.builder().type(Type.Known.STRING).description("Sugestão para thumbnail").build(),
                    "best_moment", Schema.builder().type(Type.Known.STRING).description("Melhor momento").build()
                ))
                .build();
            
            GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(geminiSettings.getTemperature())
                .maxOutputTokens(geminiSettings.getMaxOutputTokens())
                .topP(geminiSettings.getTopP())
                .topK((float) geminiSettings.getTopK())
                .responseMimeType("application/json")
                .responseSchema(responseSchema)
                .build();

            // Usando a API correta: client.models.generateContent
            GenerateContentResponse response = geminiClient.models.generateContent(
                geminiSettings.getModelName(),
                prompt,
                config
            );

            return parseAnalysisResponse(response.text());

        } catch (Exception e) {
            log.error("❌ Erro ao analisar clip: {}", e.getMessage());
            return createFallbackAnalysis(clipTitle, clipDescription, streamerName, gameName);
        }
    }

    /**
     * Análise assíncrona de clip usando client.async.models
     */
    public CompletableFuture<ClipAnalysis> analyzeClipAsync(String clipTitle, String clipDescription, 
                                                           String streamerName, String gameName) {
        
        log.info("🔄 Iniciando análise assíncrona para: {}", clipTitle);
        
        String prompt = buildAnalysisPrompt(clipTitle, clipDescription, streamerName, gameName);
        
        // Configuração com schema JSON
        Schema responseSchema = Schema.builder()
            .type("object")
            .properties(ImmutableMap.of(
                "title", Schema.builder().type(Type.Known.STRING).description("Título otimizado").build(),
                "description", Schema.builder().type(Type.Known.STRING).description("Descrição detalhada").build(),
                "tags", Schema.builder()
                    .type("array")
                    .items(Schema.builder().type(Type.Known.STRING).build())
                    .description("Lista de tags").build(),
                "category", Schema.builder().type(Type.Known.STRING).description("Categoria do clip").build(),
                "thumbnail_suggestion", Schema.builder().type(Type.Known.STRING).description("Sugestão para thumbnail").build(),
                "best_moment", Schema.builder().type(Type.Known.STRING).description("Melhor momento").build()
            ))
            .build();
        
        GenerateContentConfig config = GenerateContentConfig.builder()
            .temperature(geminiSettings.getTemperature())
            .maxOutputTokens(geminiSettings.getMaxOutputTokens())
            .topP(geminiSettings.getTopP())
            .topK((float) geminiSettings.getTopK())
            .responseMimeType("application/json")
            .responseSchema(responseSchema)
            .build();

        // Usando client.async.models conforme documentação
        return geminiClient.async.models.generateContent(
            geminiSettings.getModelName(),
            prompt,
            config
        ).thenApply(response -> {
            try {
                return parseAnalysisResponse(response.text());
            } catch (Exception e) {
                log.error("❌ Erro ao processar resposta assíncrona: {}", e.getMessage());
                return createFallbackAnalysis(clipTitle, clipDescription, streamerName, gameName);
            }
        }).exceptionally(ex -> {
            log.error("❌ Erro na análise assíncrona: {}", ex.getMessage());
            return createFallbackAnalysis(clipTitle, clipDescription, streamerName, gameName);
        });
    }

    /**
     * Gera apenas um título otimizado
     */
    public String generateOptimizedTitle(String originalTitle, String streamerName, String gameName) {
        try {
            String prompt = String.format("""
                Crie um título CHAMATIVO e OTIMIZADO para YouTube baseado nestas informações:
                
                Título original: %s
                Streamer: %s
                Jogo: %s
                
                Regras:
                - Máximo 100 caracteres
                - Use CAPS para palavras-chave importantes
                - Inclua emojis relevantes
                - Seja clickbait mas honesto
                - Foque no momento mais interessante
                
                Responda apenas com o título, sem explicações.
                """, originalTitle, streamerName, gameName);

            // Usando client.models.generateContent
            GenerateContentResponse response = geminiClient.models.generateContent(
                geminiSettings.getModelName(),
                prompt,
                null
            );

            return response.text().trim();

        } catch (Exception e) {
            log.error("❌ Erro ao gerar título: {}", e.getMessage());
            return originalTitle; // Fallback para título original
        }
    }

    /**
     * Gera tags otimizadas para o clip
     */
    public List<String> generateTags(String clipTitle, String streamerName, String gameName) {
        try {
            String prompt = String.format("""
                Gere tags otimizadas para YouTube baseado nestas informações:
                
                Título: %s
                Streamer: %s
                Jogo: %s
                
                Regras:
                - Entre 10-15 tags
                - Mix de tags específicas e gerais
                - Inclua nome do streamer e jogo
                - Use palavras-chave populares
                - Formato: uma tag por linha
                
                Responda apenas com as tags, uma por linha.
                """, clipTitle, streamerName, gameName);

            // Usando client.models.generateContent
            GenerateContentResponse response = geminiClient.models.generateContent(
                geminiSettings.getModelName(),
                prompt,
                null
            );

            String[] tagArray = response.text().split("\n");
            List<String> tags = new ArrayList<>();
            for (String tag : tagArray) {
                String cleanTag = tag.trim();
                if (!cleanTag.isEmpty()) {
                    tags.add(cleanTag);
                }
            }
            return tags;

        } catch (Exception e) {
            log.error("❌ Erro ao gerar tags: {}", e.getMessage());
            return List.of(streamerName, gameName, "gaming", "clip", "highlight");
        }
    }

    /**
     * Análise de sentimento do clip
     */
    public ClipSentiment analyzeSentiment(String clipTitle, String clipDescription) {
        try {
            String prompt = String.format("""
                Analise o sentimento deste clip:
                
                Título: %s
                Descrição: %s
                
                Classifique como:
                - POSITIVE (engraçado, impressionante, emocionante)
                - NEGATIVE (frustrante, triste, raiva)
                - NEUTRAL (informativo, normal)
                
                Responda apenas com: POSITIVE, NEGATIVE ou NEUTRAL
                """, clipTitle, clipDescription);

            // Usando client.models.generateContent
            GenerateContentResponse response = geminiClient.models.generateContent(
                geminiSettings.getModelName(),
                prompt,
                null
            );

            String sentiment = response.text().trim().toUpperCase();
            return ClipSentiment.valueOf(sentiment);

        } catch (Exception e) {
            log.error("❌ Erro ao analisar sentimento: {}", e.getMessage());
            return ClipSentiment.NEUTRAL;
        }
    }

    /**
     * Exemplo de Function Calling para análise avançada de clips
     * Implementa automatic function calling conforme documentação
     */
    public ClipAnalysis analyzeClipWithFunctionCalling(String clipTitle, String clipDescription, 
                                                      String streamerName, String gameName) {
        try {
            log.info("🔧 Analisando clip com Function Calling: {}", clipTitle);

            // Obter os métodos via reflection conforme documentação
            java.lang.reflect.Method getClipCategoryMethod = 
                GeminiAnalysisService.class.getMethod("getClipCategory", String.class, String.class, String.class);
            
            java.lang.reflect.Method generateSeoTagsMethod = 
                GeminiAnalysisService.class.getMethod("generateSeoTags", String.class, String.class);
            
            java.lang.reflect.Method calculateViralScoreMethod = 
                GeminiAnalysisService.class.getMethod("calculateViralScore", String.class, String.class);

            // Configuração com tools para function calling
            com.google.genai.types.Tool tool = com.google.genai.types.Tool.builder()
                .functions(com.google.common.collect.ImmutableList.of(
                    getClipCategoryMethod,
                    generateSeoTagsMethod,
                    calculateViralScoreMethod
                ))
                .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(geminiSettings.getTemperature())
                .maxOutputTokens(geminiSettings.getMaxOutputTokens())
                .tools(com.google.common.collect.ImmutableList.of(tool))
                .build();

            String prompt = String.format("""
                Analise este clip da Twitch usando as funções disponíveis:
                
                Título: %s
                Descrição: %s
                Streamer: %s
                Jogo: %s
                
                Por favor:
                1. Use getClipCategory para determinar a categoria
                2. Use generateSeoTags para gerar tags SEO
                3. Use calculateViralScore para calcular potencial viral
                
                Depois forneça uma análise completa baseada nos resultados das funções.
                Inclua os resultados obtidos e suas interpretações sobre o potencial do clip.
                """, clipTitle, clipDescription, streamerName, gameName);

            GenerateContentResponse response = geminiClient.models.generateContent(
                geminiSettings.getModelName(),
                prompt,
                config
            );

            // Log do histórico de function calling
            if (response.automaticFunctionCallingHistory().isPresent()) {
                log.info("📞 Function calling history: {}", 
                    response.automaticFunctionCallingHistory().get());
            }

            // Com function calling, a resposta será em texto, não JSON
            // Vamos usar createAnalysisFromTextResponse para extrair informações
            String responseText = response.text();
            log.info("📝 Resposta do Gemini com function calling: {}", responseText);
            
            return createAnalysisFromTextResponse(responseText, clipTitle, clipDescription, streamerName, gameName);

        } catch (Exception e) {
            log.error("❌ Erro na análise com function calling: {}", e.getMessage());
            return createFallbackAnalysis(clipTitle, clipDescription, streamerName, gameName);
        }
    }

    /**
     * Função para ser chamada automaticamente pelo Gemini - determina categoria do clip
     */
    public static String getClipCategory(String title, String description, String gameName) {
        // Lógica simples para categorização
        String content = (title + " " + description).toLowerCase();
        
        if (content.contains("fail") || content.contains("morte") || content.contains("bug")) {
            return "FAIL";
        } else if (content.contains("win") || content.contains("vitória") || content.contains("clutch")) {
            return "EPIC";
        } else if (content.contains("funny") || content.contains("engraçado") || content.contains("lol")) {
            return "FUNNY";
        } else if (content.contains("tutorial") || content.contains("dica") || content.contains("guide")) {
            return "EDUCATIONAL";
        } else {
            return "IMPRESSIVE";
        }
    }

    /**
     * Função para ser chamada automaticamente pelo Gemini - gera tags SEO
     */
    public static String generateSeoTags(String gameName, String category) {
        List<String> baseTags = new ArrayList<>();
        baseTags.add(gameName.toLowerCase());
        baseTags.add("gaming");
        baseTags.add("twitch");
        baseTags.add("clip");
        
        // Tags baseadas na categoria
        switch (category.toUpperCase()) {
            case "FAIL":
                baseTags.addAll(List.of("fail", "funny", "moments", "epic fail"));
                break;
            case "EPIC":
                baseTags.addAll(List.of("epic", "amazing", "clutch", "highlight"));
                break;
            case "FUNNY":
                baseTags.addAll(List.of("funny", "humor", "comedy", "laugh"));
                break;
            case "EDUCATIONAL":
                baseTags.addAll(List.of("tutorial", "guide", "tips", "learn"));
                break;
            default:
                baseTags.addAll(List.of("gameplay", "moments", "highlights"));
        }
        
        return String.join(", ", baseTags);
    }

    /**
     * Função para ser chamada automaticamente pelo Gemini - calcula score viral
     */
    public static String calculateViralScore(String title, String category) {
        int score = 50; // Base score
        
        // Fatores que aumentam o score
        if (title.toLowerCase().contains("impossible")) score += 20;
        if (title.toLowerCase().contains("insane")) score += 15;
        if (title.toLowerCase().contains("epic")) score += 15;
        if (title.toUpperCase().equals(title)) score += 10; // All caps
        if (title.contains("!")) score += 5;
        
        // Fatores baseados na categoria
        switch (category.toUpperCase()) {
            case "EPIC": score += 20; break;
            case "FUNNY": score += 15; break;
            case "FAIL": score += 10; break;
        }
        
        // Limitar entre 0-100
        score = Math.max(0, Math.min(100, score));
        
        return String.valueOf(score);
    }

    private String buildAnalysisPrompt(String clipTitle, String clipDescription, String streamerName, String gameName) {
        return String.format("""
            Analise este clip da Twitch e gere conteúdo otimizado para YouTube:
            
            DADOS DO CLIP:
            - Título: %s
            - Descrição: %s
            - Streamer: %s
            - Jogo: %s
            
            GERE O SEGUINTE CONTEÚDO (formato JSON):
            {
              "title": "Título otimizado (máx 100 chars, clickbait mas honesto)",
              "description": "Descrição detalhada (200-500 chars) com contexto e call-to-action",
              "tags": ["tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8"],
              "category": "FUNNY|IMPRESSIVE|EPIC|FAIL|EDUCATIONAL",
              "thumbnail_suggestion": "Sugestão para thumbnail",
              "best_moment": "Timestamp ou momento mais interessante"
            }
            
            RESPONDA APENAS COM O JSON, SEM EXPLICAÇÕES ADICIONAIS.
            """, clipTitle, clipDescription, streamerName, gameName);
    }

    /**
     * Parser JSON robusto usando Jackson
     */
    private ClipAnalysis parseAnalysisResponse(String jsonResponse) {
        try {
            log.debug("📝 Resposta do Gemini: {}", jsonResponse);
            
            // Parse do JSON usando Jackson
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            
            String title = jsonNode.has("title") ? jsonNode.get("title").asText() : "Título não gerado";
            String description = jsonNode.has("description") ? jsonNode.get("description").asText() : "Descrição não gerada";
            String category = jsonNode.has("category") ? jsonNode.get("category").asText() : "GENERAL";
            String thumbnailSuggestion = jsonNode.has("thumbnail_suggestion") ? jsonNode.get("thumbnail_suggestion").asText() : "Verificar manualmente";
            String bestMoment = jsonNode.has("best_moment") ? jsonNode.get("best_moment").asText() : "Verificar manualmente";
            
            // Parse dos novos campos
            Double viralScore = jsonNode.has("viral_score") ? jsonNode.get("viral_score").asDouble() : 5.0;
            String sentiment = jsonNode.has("sentiment") ? jsonNode.get("sentiment").asText() : "NEUTRAL";
            Integer estimatedViews = jsonNode.has("estimated_views") ? jsonNode.get("estimated_views").asInt() : 1000;
            String bestUploadTime = jsonNode.has("best_upload_time") ? jsonNode.get("best_upload_time").asText() : "18:00";
            
            // Parse das tags
            List<String> tags = new ArrayList<>();
            if (jsonNode.has("tags") && jsonNode.get("tags").isArray()) {
                for (JsonNode tagNode : jsonNode.get("tags")) {
                    tags.add(tagNode.asText());
                }
            } else {
                tags = List.of("gaming", "twitch", "clip", "highlight");
            }
            
            // Parse das hashtags sociais
            List<String> socialHashtags = new ArrayList<>();
            if (jsonNode.has("social_hashtags") && jsonNode.get("social_hashtags").isArray()) {
                for (JsonNode hashtagNode : jsonNode.get("social_hashtags")) {
                    socialHashtags.add(hashtagNode.asText());
                }
            } else {
                // Gerar hashtags baseadas nas tags
                socialHashtags = tags.stream()
                    .limit(4)
                    .map(tag -> "#" + tag.replace(" ", ""))
                    .toList();
            }
            
            return ClipAnalysis.builder()
                .optimizedTitle(title)
                .optimizedDescription(description)
                .tags(tags)
                .category(category)
                .thumbnailSuggestion(thumbnailSuggestion)
                .bestMoment(bestMoment)
                .viralScore(viralScore)
                .sentiment(sentiment)
                .estimatedViews(estimatedViews)
                .bestUploadTime(bestUploadTime)
                .socialHashtags(socialHashtags)
                .build();
                
        } catch (Exception e) {
            log.error("❌ Erro ao parsear resposta JSON do Gemini: {}", e.getMessage());
            log.error("❌ JSON recebido: {}", jsonResponse);
            
            // Ao invés de lançar exceção, tentar extrair informações da resposta
            log.warn("⚠️ Tentando extrair informações da resposta como texto");
            return createAnalysisFromTextResponse(jsonResponse, "Título não identificado", 
                                                "Descrição não identificada", "null", "null");
        }
    }

    private ClipAnalysis createFallbackAnalysis(String clipTitle, String clipDescription, 
                                              String streamerName, String gameName) {
        return ClipAnalysis.builder()
            .optimizedTitle(clipTitle + " - " + streamerName)
            .optimizedDescription("Clip incrível de " + streamerName + " jogando " + gameName + ". " + 
                                (clipDescription != null ? clipDescription : "Momento épico de gameplay!"))
            .tags(List.of(streamerName, gameName, "gaming", "twitch", "clip"))
            .category("GAMING")
            .thumbnailSuggestion("Usar frame do melhor momento")
            .bestMoment("Verificar manualmente")
            .viralScore(5.0)
            .sentiment("NEUTRAL")
            .estimatedViews(1000)
            .bestUploadTime("18:00")
            .socialHashtags(List.of("#" + gameName.replace(" ", ""), "#" + streamerName, "#gaming", "#twitch"))
            .build();
    }

    /**
     * Cria uma análise baseada em resposta de texto do Gemini (não JSON)
     * Extrai informações úteis da resposta em markdown/texto
     */
    private ClipAnalysis createAnalysisFromTextResponse(String textResponse, String clipTitle, 
                                                       String clipDescription, String streamerName, String gameName) {
        try {
            log.info("📝 Extraindo informações da resposta de texto");
            
            // Extrair score viral se mencionado na resposta
            Double viralScore = 50.0; // default
            if (textResponse.contains("pontuação de ")) {
                try {
                    String scoreText = textResponse.substring(textResponse.indexOf("pontuação de ") + 13);
                    scoreText = scoreText.substring(0, scoreText.indexOf(" ")).trim();
                    viralScore = Double.parseDouble(scoreText);
                } catch (Exception e) {
                    log.debug("Não foi possível extrair score viral do texto");
                }
            }
            // Também procurar por patterns como "score: 50" ou "pontuação: 50"
            if (textResponse.matches(".*(?:score|pontuação).*?(\\d+).*")) {
                try {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:score|pontuação).*?(\\d+)");
                    java.util.regex.Matcher matcher = pattern.matcher(textResponse);
                    if (matcher.find()) {
                        viralScore = Double.parseDouble(matcher.group(1));
                    }
                } catch (Exception e) {
                    log.debug("Erro ao extrair score com regex");
                }
            }
            
            // Extrair categoria se mencionada - incluir mais variações
            String category = "IMPRESSIVE"; // default
            String lowerResponse = textResponse.toLowerCase();
            if (lowerResponse.contains("impressive") || lowerResponse.contains("impressionante")) {
                category = "IMPRESSIVE";
            } else if (lowerResponse.contains("epic") || lowerResponse.contains("épico")) {
                category = "EPIC";
            } else if (lowerResponse.contains("funny") || lowerResponse.contains("engraçado") || lowerResponse.contains("humor")) {
                category = "FUNNY";
            } else if (lowerResponse.contains("fail") || lowerResponse.contains("falha")) {
                category = "FAIL";
            } else if (lowerResponse.contains("educational") || lowerResponse.contains("educativo")) {
                category = "EDUCATIONAL";
            }
            
            // Gerar tags baseadas no contexto e extrair da resposta
            List<String> tags = new ArrayList<>();
            if (streamerName != null && !streamerName.equals("null")) {
                tags.add(streamerName);
            }
            if (gameName != null && !gameName.equals("null")) {
                tags.add(gameName);
            }
            
            // Extrair tags mencionadas na resposta
            if (textResponse.contains("tags:") || textResponse.contains("Tags:")) {
                String tagsSection = textResponse.substring(textResponse.toLowerCase().indexOf("tags:"));
                String[] potentialTags = tagsSection.split("[,\\s]+");
                for (String tag : potentialTags) {
                    String cleanTag = tag.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                    if (cleanTag.length() > 2 && !tags.contains(cleanTag)) {
                        tags.add(cleanTag);
                    }
                }
            }
            
            // Adicionar tags padrão se não temos o suficiente
            if (tags.size() < 5) {
                List<String> defaultTags = List.of("gaming", "twitch", "clip", "gameplay", "moments", "highlights");
                for (String defaultTag : defaultTags) {
                    if (!tags.contains(defaultTag) && tags.size() < 8) {
                        tags.add(defaultTag);
                    }
                }
            }
            
            // Criar título otimizado baseado no original e categoria
            String optimizedTitle = clipTitle;
            if (clipTitle.equals("kkkk") || clipTitle.length() < 10) {
                optimizedTitle = category + " Moment";
                if (streamerName != null && !streamerName.equals("null")) {
                    optimizedTitle += " - " + streamerName;
                }
                if (gameName != null && !gameName.equals("null")) {
                    optimizedTitle += " " + gameName;
                }
            } else {
                // Melhorar título existente
                optimizedTitle = clipTitle + " - " + category + " CLIP";
                if (streamerName != null && !streamerName.equals("null")) {
                    optimizedTitle += " | " + streamerName;
                }
            }
            
            // Criar descrição baseada na análise
            String optimizedDescription = String.format(
                "🎮 Clip %s de %s! Categoria: %s | Score viral: %.0f | %s",
                category.toLowerCase(),
                streamerName != null && !streamerName.equals("null") ? streamerName : "gaming",
                category,
                viralScore,
                gameName != null && !gameName.equals("null") ? "Jogo: " + gameName + " |" : ""
            );
            
            // Adicionar contexto da análise se disponível
            if (textResponse.length() > 100) {
                String context = textResponse.substring(0, Math.min(200, textResponse.length()));
                optimizedDescription += " Análise: " + context.replaceAll("\\s+", " ").trim();
            }
            
            return ClipAnalysis.builder()
                .optimizedTitle(optimizedTitle)
                .optimizedDescription(optimizedDescription)
                .tags(tags)
                .category(category)
                .thumbnailSuggestion("Usar frame destacando o momento " + category.toLowerCase())
                .bestMoment("Verificar manualmente")
                .viralScore(viralScore)
                .sentiment("POSITIVE")
                .estimatedViews((int) (viralScore * 20)) // Estimativa baseada no score
                .bestUploadTime("18:00")
                .socialHashtags(tags.stream().limit(4).map(tag -> "#" + tag.replace(" ", "")).toList())
                .build();
                
        } catch (Exception e) {
            log.error("❌ Erro ao extrair informações da resposta de texto: {}", e.getMessage());
            return createFallbackAnalysis(clipTitle, clipDescription, streamerName, gameName);
        }
    }

    // Classes de dados para as respostas
    
    public static class ClipAnalysis {
        private String optimizedTitle;
        private String optimizedDescription;
        private List<String> tags;
        private String category;
        private String thumbnailSuggestion;
        private String bestMoment;
        private Double viralScore;
        private String sentiment;
        private Integer estimatedViews;
        private String bestUploadTime;
        private List<String> socialHashtags;

        public static ClipAnalysisBuilder builder() {
            return new ClipAnalysisBuilder();
        }

        // Getters
        public String getOptimizedTitle() { return optimizedTitle; }
        public String getOptimizedDescription() { return optimizedDescription; }
        public List<String> getTags() { return tags != null ? tags : new ArrayList<>(); }
        public String getCategory() { return category; }
        public String getThumbnailSuggestion() { return thumbnailSuggestion; }
        public String getBestMoment() { return bestMoment; }
        public Double getViralScore() { return viralScore != null ? viralScore : 0.0; }
        public String getSentiment() { return sentiment != null ? sentiment : "NEUTRAL"; }
        public Integer getEstimatedViews() { return estimatedViews != null ? estimatedViews : 0; }
        public String getBestUploadTime() { return bestUploadTime != null ? bestUploadTime : "18:00"; }
        public List<String> getSocialHashtags() { return socialHashtags != null ? socialHashtags : new ArrayList<>(); }

        // Setters
        public void setOptimizedTitle(String optimizedTitle) { this.optimizedTitle = optimizedTitle; }
        public void setOptimizedDescription(String optimizedDescription) { this.optimizedDescription = optimizedDescription; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public void setCategory(String category) { this.category = category; }
        public void setThumbnailSuggestion(String thumbnailSuggestion) { this.thumbnailSuggestion = thumbnailSuggestion; }
        public void setBestMoment(String bestMoment) { this.bestMoment = bestMoment; }
        public void setViralScore(Double viralScore) { this.viralScore = viralScore; }
        public void setSentiment(String sentiment) { this.sentiment = sentiment; }
        public void setEstimatedViews(Integer estimatedViews) { this.estimatedViews = estimatedViews; }
        public void setBestUploadTime(String bestUploadTime) { this.bestUploadTime = bestUploadTime; }
        public void setSocialHashtags(List<String> socialHashtags) { this.socialHashtags = socialHashtags; }

        public static class ClipAnalysisBuilder {
            private ClipAnalysis analysis = new ClipAnalysis();

            public ClipAnalysisBuilder optimizedTitle(String title) {
                analysis.optimizedTitle = title;
                return this;
            }

            public ClipAnalysisBuilder optimizedDescription(String description) {
                analysis.optimizedDescription = description;
                return this;
            }

            public ClipAnalysisBuilder tags(List<String> tags) {
                analysis.tags = tags;
                return this;
            }

            public ClipAnalysisBuilder category(String category) {
                analysis.category = category;
                return this;
            }

            public ClipAnalysisBuilder thumbnailSuggestion(String suggestion) {
                analysis.thumbnailSuggestion = suggestion;
                return this;
            }

            public ClipAnalysisBuilder bestMoment(String moment) {
                analysis.bestMoment = moment;
                return this;
            }

            public ClipAnalysisBuilder viralScore(Double score) {
                analysis.viralScore = score;
                return this;
            }

            public ClipAnalysisBuilder sentiment(String sentiment) {
                analysis.sentiment = sentiment;
                return this;
            }

            public ClipAnalysisBuilder estimatedViews(Integer views) {
                analysis.estimatedViews = views;
                return this;
            }

            public ClipAnalysisBuilder bestUploadTime(String time) {
                analysis.bestUploadTime = time;
                return this;
            }

            public ClipAnalysisBuilder socialHashtags(List<String> hashtags) {
                analysis.socialHashtags = hashtags;
                return this;
            }

            public ClipAnalysis build() {
                return analysis;
            }
        }
    }

    public enum ClipSentiment {
        POSITIVE, NEGATIVE, NEUTRAL
    }

    /**
     * Análise streaming de clip - gera resposta em tempo real
     */
    public void analyzeClipStream(String clipTitle, String clipDescription, String streamerName, 
                                 String gameName, java.util.function.Consumer<String> onPartialResponse) {
        try {
            log.info("🌊 Iniciando análise streaming para: {}", clipTitle);

            String prompt = buildAnalysisPrompt(clipTitle, clipDescription, streamerName, gameName);
            
            GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(geminiSettings.getTemperature())
                .maxOutputTokens(geminiSettings.getMaxOutputTokens())
                .topP(geminiSettings.getTopP())
                .topK((float) geminiSettings.getTopK())
                .build();

            // Usando generateContentStream conforme documentação
            com.google.genai.ResponseStream<GenerateContentResponse> responseStream = 
                geminiClient.models.generateContentStream(
                    geminiSettings.getModelName(),
                    prompt,
                    config
                );

            log.info("📡 Streaming response iniciado");
            
            try {
                for (GenerateContentResponse response : responseStream) {
                    if (response.text() != null && !response.text().isEmpty()) {
                        onPartialResponse.accept(response.text());
                    }
                }
            } finally {
                // Importante: fechar o stream para evitar vazamentos de conexão
                responseStream.close();
                log.info("📡 Stream finalizado");
            }

        } catch (Exception e) {
            log.error("❌ Erro na análise streaming: {}", e.getMessage());
            onPartialResponse.accept("Erro: " + e.getMessage());
        }
    }

    /**
     * Análise multimodal com thumbnail do clip
     * Conforme exemplo da documentação para text + image input
     */
    public ClipAnalysis analyzeClipWithThumbnail(String clipTitle, String clipDescription, 
                                                String streamerName, String gameName, 
                                                String thumbnailUri, String mimeType) {
        try {
            log.info("🖼️ Analisando clip com thumbnail: {}", clipTitle);

            // Construir conteúdo multimodal conforme documentação
            Content content = Content.fromParts(
                Part.fromText(String.format("""
                    Analise este clip da Twitch usando a thumbnail e as informações:
                    
                    Título: %s
                    Descrição: %s
                    Streamer: %s
                    Jogo: %s
                    
                    Baseado na thumbnail, gere:
                    1. Título otimizado que capture o momento visual
                    2. Descrição que destaque o que está acontecendo na imagem
                    3. Tags relevantes para o conteúdo visual
                    4. Sugestão de melhoria da thumbnail
                    
                    Responda em formato JSON.
                    """, clipTitle, clipDescription, streamerName, gameName)),
                Part.fromUri(thumbnailUri, mimeType)
            );

            // Schema para resposta estruturada
            Schema responseSchema = Schema.builder()
                .type("object")
                .properties(ImmutableMap.of(
                    "title", Schema.builder().type(Type.Known.STRING).description("Título otimizado baseado na thumbnail").build(),
                    "description", Schema.builder().type(Type.Known.STRING).description("Descrição baseada no conteúdo visual").build(),
                    "tags", Schema.builder()
                        .type("array")
                        .items(Schema.builder().type(Type.Known.STRING).build())
                        .description("Tags baseadas no conteúdo visual").build(),
                    "visual_analysis", Schema.builder().type(Type.Known.STRING).description("Análise do conteúdo visual").build(),
                    "thumbnail_improvement", Schema.builder().type(Type.Known.STRING).description("Sugestões de melhoria").build()
                ))
                .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(geminiSettings.getTemperature())
                .maxOutputTokens(geminiSettings.getMaxOutputTokens())
                .responseMimeType("application/json")
                .responseSchema(responseSchema)
                .build();

            // Usando content multimodal conforme documentação
            GenerateContentResponse response = geminiClient.models.generateContent(
                geminiSettings.getModelName(),
                content,
                config
            );

            return parseMultimodalAnalysisResponse(response.text());

        } catch (Exception e) {
            log.error("❌ Erro na análise multimodal: {}", e.getMessage());
            return createFallbackAnalysis(clipTitle, clipDescription, streamerName, gameName);
        }
    }

    /**
     * Parser específico para análise multimodal
     */
    private ClipAnalysis parseMultimodalAnalysisResponse(String jsonResponse) {
        try {
            log.debug("📝 Resposta multimodal do Gemini: {}", jsonResponse);
            
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            
            String title = jsonNode.has("title") ? jsonNode.get("title").asText() : "Título não gerado";
            String description = jsonNode.has("description") ? jsonNode.get("description").asText() : "Descrição não gerada";
            String visualAnalysis = jsonNode.has("visual_analysis") ? jsonNode.get("visual_analysis").asText() : "Análise visual não disponível";
            String thumbnailImprovement = jsonNode.has("thumbnail_improvement") ? jsonNode.get("thumbnail_improvement").asText() : "Sem sugestões";
            
            // Parse das tags
            List<String> tags = new ArrayList<>();
            if (jsonNode.has("tags") && jsonNode.get("tags").isArray()) {
                for (JsonNode tagNode : jsonNode.get("tags")) {
                    tags.add(tagNode.asText());
                }
            } else {
                tags = List.of("gaming", "twitch", "clip", "visual");
            }
            
            return ClipAnalysis.builder()
                .optimizedTitle(title)
                .optimizedDescription(description + "\n\nAnálise visual: " + visualAnalysis)
                .tags(tags)
                .category("VISUAL")
                .thumbnailSuggestion(thumbnailImprovement)
                .bestMoment("Conforme thumbnail analisada")
                .viralScore(6.5) // Score ligeiramente maior para análise visual
                .sentiment("POSITIVE") // Assumir positivo se teve thumbnail
                .estimatedViews(1500) // Mais views por análise visual
                .bestUploadTime("19:00") // Horário prime time
                .socialHashtags(tags.stream().limit(4).map(tag -> "#" + tag.replace(" ", "")).toList())
                .build();
                
        } catch (Exception e) {
            log.error("❌ Erro ao parsear resposta multimodal: {}", e.getMessage());
            throw new RuntimeException("Falha ao processar análise multimodal", e);
        }
    }

    /**
     * Análise avançada com Google Search e Safety Settings
     * Conforme exemplo da documentação "Generate Content with extra configs"
     */
    public ClipAnalysis analyzeClipWithGoogleSearch(String clipTitle, String clipDescription, 
                                                   String streamerName, String gameName) {
        try {
            log.info("🔍 Analisando clip com Google Search: {}", clipTitle);

            // Configurar Safety Settings conforme documentação
            java.util.List<com.google.genai.types.SafetySetting> safetySettings = 
                com.google.common.collect.ImmutableList.of(
                    com.google.genai.types.SafetySetting.builder()
                        .category(com.google.genai.types.HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH)
                        .threshold(com.google.genai.types.HarmBlockThreshold.Known.BLOCK_ONLY_HIGH)
                        .build(),
                    com.google.genai.types.SafetySetting.builder()
                        .category(com.google.genai.types.HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT)
                        .threshold(com.google.genai.types.HarmBlockThreshold.Known.BLOCK_LOW_AND_ABOVE)
                        .build()
                );

            // System instruction conforme documentação
            Content systemInstruction = Content.fromParts(
                Part.fromText("Você é um especialista em análise de conteúdo gaming e otimização para YouTube. " +
                            "Use informações atualizadas da web para fornecer análises mais precisas.")
            );

            // Google Search Tool conforme documentação
            com.google.genai.types.Tool googleSearchTool = com.google.genai.types.Tool.builder()
                .googleSearch(com.google.genai.types.GoogleSearch.builder().build())
                .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                .candidateCount(1)
                .maxOutputTokens(1024)
                .temperature(geminiSettings.getTemperature())
                .safetySettings(safetySettings)
                .systemInstruction(systemInstruction)
                .tools(com.google.common.collect.ImmutableList.of(googleSearchTool))
                .build();

            String prompt = String.format("""
                Analise este clip considerando tendências atuais de gaming e YouTube:
                
                Título: %s
                Descrição: %s
                Streamer: %s
                Jogo: %s
                
                Por favor, busque informações atuais sobre:
                1. Tendências do jogo %s no YouTube
                2. Estratégias de SEO para gaming content
                3. Palavras-chave populares relacionadas
                
                Baseado nas informações encontradas, gere um título e descrição otimizados.
                """, clipTitle, clipDescription, streamerName, gameName, gameName);

            GenerateContentResponse response = geminiClient.models.generateContent(
                geminiSettings.getModelName(),
                prompt,
                config
            );

            // Parse da resposta e criação da análise
            String responseText = response.text();
            
            // Extrair informações básicas da resposta
            return ClipAnalysis.builder()
                .optimizedTitle(extractTitleFromResponse(responseText, clipTitle))
                .optimizedDescription(extractDescriptionFromResponse(responseText, clipDescription))
                .tags(extractTagsFromResponse(responseText, gameName, streamerName))
                .category("TRENDING")
                .thumbnailSuggestion("Use elementos visuais em alta no " + gameName)
                .bestMoment("Baseado em análise de tendências atuais")
                .viralScore(7.5) // Score alto para análise com Google Search
                .sentiment("POSITIVE") // Assumir positivo para conteúdo trending
                .estimatedViews(2500) // Mais views por usar tendências atuais
                .bestUploadTime("20:00") // Horário prime para gaming content
                .socialHashtags(extractTagsFromResponse(responseText, gameName, streamerName)
                    .stream().limit(5).map(tag -> "#" + tag.replace(" ", "")).toList())
                .build();

        } catch (Exception e) {
            log.error("❌ Erro na análise com Google Search: {}", e.getMessage());
            return createFallbackAnalysis(clipTitle, clipDescription, streamerName, gameName);
        }
    }

    // Métodos auxiliares para extrair informações da resposta do Google Search
    private String extractTitleFromResponse(String response, String fallback) {
        // Lógica simples para extrair título da resposta
        if (response.toLowerCase().contains("título:")) {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.toLowerCase().contains("título:")) {
                    return line.substring(line.indexOf(":") + 1).trim();
                }
            }
        }
        return fallback + " - TRENDING";
    }

    private String extractDescriptionFromResponse(String response, String fallback) {
        // Lógica simples para extrair descrição da resposta
        if (response.toLowerCase().contains("descrição:")) {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.toLowerCase().contains("descrição:")) {
                    return line.substring(line.indexOf(":") + 1).trim();
                }
            }
        }
        return fallback + " | Baseado em tendências atuais";
    }

    private List<String> extractTagsFromResponse(String response, String gameName, String streamerName) {
        List<String> tags = new ArrayList<>();
        tags.add(gameName.toLowerCase());
        tags.add(streamerName.toLowerCase());
        tags.add("trending");
        tags.add("viral");
        tags.add("gaming");
        
        // Procurar por tags na resposta
        if (response.toLowerCase().contains("tags:") || response.toLowerCase().contains("palavras-chave:")) {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.toLowerCase().contains("tags:") || line.toLowerCase().contains("palavras-chave:")) {
                    String tagLine = line.substring(line.indexOf(":") + 1).trim();
                    String[] extractedTags = tagLine.split(",");
                    for (String tag : extractedTags) {
                        String cleanTag = tag.trim().replaceAll("[\"'\\[\\]]", "");
                        if (!cleanTag.isEmpty() && !tags.contains(cleanTag.toLowerCase())) {
                            tags.add(cleanTag.toLowerCase());
                        }
                    }
                }
            }
        }
        
        return tags;
    }
} 
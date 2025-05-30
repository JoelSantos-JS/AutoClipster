package com.joel.br.AutoClipster.config;

import com.google.genai.Client;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

/**
 * Configuração central para integração com Google Gemini
 * Usa a biblioteca oficial com.google.genai seguindo a documentação do Google
 */
@Configuration
@Slf4j
@Getter
public class GeminiConfig {

    /**
     * API Key do Gemini
     */
    @Value("${gemini.api.key}")
    private String apiKey;

    /**
     * Modelo a ser usado - permite trocar facilmente
     * gemini-2.0-flash-001 (mais novo) ou gemini-1.5-flash (estável)
     */
    @Value("${gemini.model.name:gemini-2.0-flash-001}")
    private String modelName;

    /**
     * Temperatura para criatividade (0.0 a 1.0)
     * 0.7 = bom balanço entre criatividade e consistência
     */
    @Value("${gemini.temperature:0.7}")
    private float temperature;

    /**
     * Máximo de tokens na resposta
     */
    @Value("${gemini.max-tokens:8192}")
    private int maxTokens;

    /**
     * Top P - controla diversidade
     */
    @Value("${gemini.top-p:0.9}")
    private float topP;

    /**
     * Top K - limita tokens considerados
     */
    @Value("${gemini.top-k:40}")
    private int topK;

    /**
     * Bean principal do Gemini Client
     * Usando a nova API simplificada da versão 1.1.0
     */
    @Bean
    public Client geminiClient() {
        try {
            log.info("🤖 Configurando Gemini Client...");
            
            if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("your_")) {
                throw new IllegalArgumentException("API Key do Gemini não configurada ou inválida");
            }

            // Nova API simplificada - versão 1.1.0
            Client client = Client.builder().apiKey(apiKey).build();

            log.info("✅ Gemini Client configurado com sucesso");
            log.info("📊 Usando modelo: {}", modelName);
            
            return client;

        } catch (Exception e) {
            log.error("❌ Erro ao configurar Gemini Client: {}", e.getMessage());
            log.error("💡 Verifique se GEMINI_API_KEY está configurado no .env");
            throw new RuntimeException("Falha na configuração do Gemini: " + e.getMessage(), e);
        }
    }

    /**
     * Configurações para geração de conteúdo
     * Seguindo o formato esperado pela API do Google
     */
    @Bean
    public GeminiSettings geminiSettings() {
        return GeminiSettings.builder()
            .temperature(temperature)
            .topP(topP)
            .topK(topK)
            .maxOutputTokens(maxTokens)
            .modelName(modelName)
            .build();
    }

    /**
     * Método utilitário para verificar se a configuração está válida
     */
    public boolean isConfigurationValid() {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("your_")) {
            log.warn("⚠️ API Key do Gemini não configurada ou inválida");
            return false;
        }
        
        log.info("✅ Configuração do Gemini válida");
        return true;
    }

    /**
     * Log das configurações atuais (sem mostrar dados sensíveis)
     */
    public void logConfiguration() {
        log.info("📊 Configurações do Gemini:");
        log.info("  ├── Modelo: {}", modelName);
        log.info("  ├── API Key: {}", apiKey != null && !apiKey.startsWith("your_") ? "✅ Configurado" : "❌ Ausente/Inválido");
        log.info("  ├── Temperatura: {}", temperature);
        log.info("  ├── Max Tokens: {}", maxTokens);
        log.info("  ├── Top P: {}", topP);
        log.info("  └── Top K: {}", topK);
    }

    /**
     * Método para testar a conexão com a API
     */
    public boolean testConnection(Client client) {
        try {
            if (!isConfigurationValid()) {
                return false;
            }
            
            log.info("🧪 Testando conexão com Gemini...");
            
            // Teste simples chamando generateContent através do models
            var response = client.models.generateContent(
                modelName, 
                "Teste de conexão", 
                null
            );
            
            if (response != null && response.text() != null) {
                log.info("✅ Conexão com Gemini estabelecida com sucesso");
                return true;
            } else {
                log.warn("⚠️ Resposta vazia do Gemini");
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ Falha no teste de conexão: {}", e.getMessage());
            return false;
        }
    }
}



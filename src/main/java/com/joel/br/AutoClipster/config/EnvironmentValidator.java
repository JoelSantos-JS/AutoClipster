package com.joel.br.AutoClipster.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Valida se todas as variáveis de ambiente necessárias estão configuradas
 * na inicialização da aplicação.
 */
@Slf4j
@Component
public class EnvironmentValidator {

    private final Environment environment;

    public EnvironmentValidator(Environment environment) {
        this.environment = environment;
    }

    /**
     * Lista de variáveis obrigatórias para o funcionamento da aplicação
     */
    private static final List<String> REQUIRED_VARIABLES = Arrays.asList(
        "TWITCH_CLIENT_ID",
        "TWITCH_CLIENT_SECRET", 
        "GEMINI_API_KEY",
        "DATABASE_URL",
        "DATABASE_USERNAME",
        "DATABASE_PASSWORD"
    );

    /**
     * Lista de variáveis opcionais mas recomendadas
     */
    private static final List<String> OPTIONAL_VARIABLES = Arrays.asList(
        "YOUTUBE_CLIENT_ID",
        "YOUTUBE_CLIENT_SECRET",
        "N8N_WEBHOOK_URL",
        "CLIPS_DOWNLOAD_PATH"
    );

    @EventListener(ApplicationReadyEvent.class)
    public void validateEnvironment() {
        log.info("🔍 Validating environment variables...");

        // Verificar variáveis obrigatórias
        List<String> missingRequired = REQUIRED_VARIABLES.stream()
            .filter(var -> !isVariableSet(var))
            .collect(Collectors.toList());

        if (!missingRequired.isEmpty()) {
            log.error("❌ Missing required environment variables: {}", missingRequired);
            log.error("💡 Please check your .env file and ensure all required variables are set");
            log.error("📋 Required variables: {}", REQUIRED_VARIABLES);
            throw new IllegalStateException("Missing required environment variables: " + missingRequired);
        }

        // Verificar variáveis opcionais
        List<String> missingOptional = OPTIONAL_VARIABLES.stream()
            .filter(var -> !isVariableSet(var))
            .collect(Collectors.toList());

        if (!missingOptional.isEmpty()) {
            log.warn("⚠️  Missing optional environment variables: {}", missingOptional);
            log.warn("💡 Some features may not work without these variables");
        }

        // Log de configurações (sem mostrar valores sensíveis)
        logConfiguration();

        log.info("✅ Environment validation completed successfully!");
    }

    private boolean isVariableSet(String variableName) {
        String value = environment.getProperty(variableName);
        return value != null && !value.trim().isEmpty() && !value.equals("your_" + variableName.toLowerCase() + "_here");
    }

    private void logConfiguration() {
        log.info("📊 Current configuration:");
        log.info("  ├── Twitch API: {}", isVariableSet("TWITCH_CLIENT_ID") ? "✅ Configured" : "❌ Missing");
        log.info("  ├── Gemini AI: {}", isVariableSet("GEMINI_API_KEY") ? "✅ Configured" : "❌ Missing");
        log.info("  ├── Database: {}", isVariableSet("DATABASE_URL") ? "✅ Configured" : "❌ Missing");
        log.info("  ├── YouTube API: {}", isVariableSet("YOUTUBE_CLIENT_ID") ? "✅ Configured" : "⚠️  Optional");
        log.info("  ├── N8N Integration: {}", isVariableSet("N8N_WEBHOOK_URL") ? "✅ Configured" : "⚠️  Optional");
        log.info("  ├── AI Processing: {}", environment.getProperty("AI_ENABLED", "true").equals("true") ? "✅ Enabled" : "❌ Disabled");
        log.info("  └── Download Path: {}", environment.getProperty("CLIPS_DOWNLOAD_PATH", "./downloads"));
    }
} 
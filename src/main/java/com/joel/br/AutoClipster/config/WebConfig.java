package com.joel.br.AutoClipster.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;

/**
 * Configuração para clientes HTTP e beans relacionados à web
 */
@Configuration
public class WebConfig {

    /**
     * Bean RestTemplate para requisições HTTP
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(30))
            .setReadTimeout(Duration.ofSeconds(60))
            .build();
    }
} 
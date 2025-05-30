package com.joel.br.AutoClipster.controller;

import com.joel.br.AutoClipster.model.YouTubeCredentials;
import com.joel.br.AutoClipster.services.YouTubeAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth/youtube")
@RequiredArgsConstructor
@Slf4j
public class AuthCallbackController {

    private final YouTubeAuthService authService;

    /**
     * Endpoint de callback para completar a autenticação OAuth
     * Este endpoint corresponde à URL que o Google chama após autorização
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> authCallback(
            @RequestParam String code,
            @RequestParam String state) {
        
        try {
            log.info("Completando autenticação YouTube para usuário: {}", state);
            
            YouTubeCredentials credentials = authService.completeAuthFlow(code, state);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "🎉 Autenticação concluída com sucesso!");
            response.put("channelTitle", credentials.getChannelTitle());
            response.put("channelId", credentials.getChannelId());
            response.put("userId", credentials.getUserId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao completar autenticação: ", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
} 
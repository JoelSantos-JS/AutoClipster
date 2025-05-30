package com.joel.br.AutoClipster.controller;

import com.joel.br.AutoClipster.DTO.TwitchClipDTO;
import com.joel.br.AutoClipster.DTO.TwitchUserDTO;
import com.joel.br.AutoClipster.services.TwitchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller para testar funcionalidades da API
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final TwitchService twitchService;

    /**
     * Endpoint para testar a conexão com o banco de dados
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Pong! API está funcionando!");
    }

    /**
     * Endpoint para buscar informações de um usuário da Twitch
     */
    @GetMapping("/twitch/user/{username}")
    public ResponseEntity<TwitchUserDTO> getUserByName(@PathVariable String username) {
        log.info("Buscando informações do usuário: {}", username);
        return twitchService.getUserByName(username)
                .map(ResponseEntity::ok)
                .block();
    }

    /**
     * Endpoint para buscar clipes de um canal da Twitch
     */
    @GetMapping("/twitch/clips/{channelId}")
    public ResponseEntity<List<TwitchClipDTO>> getClips(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "1") int days) {
        
        log.info("Buscando clipes do canal {} dos últimos {} dias", channelId, days);
        
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);
        
        List<TwitchClipDTO> clips = twitchService.getClipsFromChannel(channelId, startTime, endTime)
                .collectList()
                .block();
        
        return ResponseEntity.ok(clips);
    }
} 
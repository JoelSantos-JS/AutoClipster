package com.joel.br.AutoClipster.services;

import com.joel.br.AutoClipster.DTO.TwitchClipDTO;
import com.joel.br.AutoClipster.DTO.TwitchUserDTO;
import com.joel.br.AutoClipster.model.TwitchClip;
import com.joel.br.AutoClipster.model.TwitchUser;
import com.joel.br.AutoClipster.response.TwitchClipsResponse;
import com.joel.br.AutoClipster.response.TwitchTokenResponse;
import com.joel.br.AutoClipster.response.TwitchUsersResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class TwitchService {


    private final WebClient webClient;
    private final RateLimitService rateLimitService;


    private final String TWITCH_API_URL = "https://api.twitch.tv/helix/";


    @Value("${twitch.clientId}")
    private String clientId;

    @Value("${twitch.clientSecret}")
    private String clientSecret;


    private String accessToken;
    private LocalDateTime tokenExpiresAt;

    public TwitchService(WebClient.Builder webClientBuilder, RateLimitService rateLimitService) {
        this.webClient = webClientBuilder.baseUrl("https://api.twitch.tv/helix").build();
        this.rateLimitService = rateLimitService;
    }

    public Flux<TwitchClipDTO> getsClipsFromChannel(String channelId, LocalDateTime startedAt, LocalDateTime endedAt){


        return ensureTokenIsValid()
                .flatMapMany(token -> {
                    // Aplica rate limiting (100 requests por minuto para Twitch API)
                    rateLimitService.acquirePermission("twitch-api", 100, Duration.ofMinutes(1));

                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/clips")
                                    .queryParam("broadcaster_id", channelId)
                                    .queryParam("started_at", startedAt.format(DateTimeFormatter.ISO_INSTANT))
                                    .queryParam("ended_at", endedAt.format(DateTimeFormatter.ISO_INSTANT))
                                    .queryParam("first", 100) // Máximo por request
                                    .build())
                            .header("Authorization", "Bearer " + token)
                            .header("Client-Id", clientId)
                            .retrieve()
                            .bodyToMono(TwitchClipsResponse.class)
                            .flatMapMany(response -> Flux.fromIterable(response.getData()))
                            .map(this::convertToDTO);
                })
                .doOnError(error -> log.error("Erro ao buscar clipes do canal {}: {}", channelId, error.getMessage()));


    }


    public Mono<TwitchUserDTO> getUserByName(String username) {
        log.info("Buscando usuário por nome: {}", username);
        
        return ensureTokenIsValid()
                .flatMap(token -> {
                    rateLimitService.acquirePermission("twitch-api", 100, Duration.ofMinutes(1));

                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/users")
                                    .queryParam("login", username)
                                    .build())
                            .header("Authorization", "Bearer " + token)
                            .header("Client-Id", clientId)
                            .retrieve()
                            .bodyToMono(TwitchUsersResponse.class)
                            .flatMap(response -> {
                                if (response.getData() == null || response.getData().isEmpty()) {
                                    log.warn("Usuário não encontrado: {}", username);
                                    return Mono.empty();
                                }
                                log.info("Usuário encontrado: {} (ID: {})", 
                                        response.getData().get(0).getDisplayName(),
                                        response.getData().get(0).getTwitchId());
                                return Mono.just(convertUserToDTO(response.getData().get(0)));
                            });
                })
                .doOnError(error -> log.error("Erro ao buscar usuário {}: {}", username, error.getMessage()));
    }


    public Flux<TwitchClipDTO> getClipsFromChannel(String channelId, LocalDateTime startedAt, LocalDateTime endedAt) {
        log.info("Buscando clipes para canal ID: {}, de {} até {}", 
                 channelId, startedAt, endedAt);
        
        return ensureTokenIsValid()
            .flatMapMany(token -> {
                log.info("Token válido obtido: {}", token.substring(0, 10) + "...");
                
                // Aplicar rate limiting
                rateLimitService.acquirePermission("twitch-api", 100, Duration.ofMinutes(1));
                
                // Formatar as datas no formato ISO 8601 que a Twitch espera
                String startedAtFormatted = startedAt.atZone(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);
                String endedAtFormatted = endedAt.atZone(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);
                
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/clips")
                        .queryParam("broadcaster_id", channelId)
                        .queryParam("started_at", startedAtFormatted)
                        .queryParam("ended_at", endedAtFormatted)
                        .queryParam("first", 100) // Máximo por request
                        .build())
                    .header("Authorization", "Bearer " + token)
                    .header("Client-Id", clientId)
                    .retrieve()
                    .bodyToMono(TwitchClipsResponse.class)
                    .doOnNext(response -> {
                        if (response.getData() == null || response.getData().isEmpty()) {
                            log.warn("Nenhum clipe encontrado para o canal {} no período especificado", channelId);
                        } else {
                            log.info("Encontrados {} clipes para o canal {}", 
                                    response.getData().size(), channelId);
                        }
                    })
                    .flatMapMany(response -> Flux.fromIterable(response.getData()))
                    .map(this::convertToDTO);
            })
            .doOnError(error -> log.error("Erro ao buscar clipes do canal {}: {}", 
                                         channelId, error.getMessage()));
    }

    /**
     * Busca um clip específico pelo ID
     */
    public Mono<TwitchClipDTO> getClipById(String clipId) {
        log.info("Buscando clip por ID: {}", clipId);
        
        return ensureTokenIsValid()
            .flatMap(token -> {
                log.info("Token válido obtido: {}", token.substring(0, 10) + "...");
                
                // Aplicar rate limiting
                rateLimitService.acquirePermission("twitch-api", 100, Duration.ofMinutes(1));
                
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/clips")
                        .queryParam("id", clipId)
                        .build())
                    .header("Authorization", "Bearer " + token)
                    .header("Client-Id", clientId)
                    .retrieve()
                    .bodyToMono(TwitchClipsResponse.class)
                    .flatMap(response -> {
                        if (response.getData() == null || response.getData().isEmpty()) {
                            log.warn("Nenhum clip encontrado com ID: {}", clipId);
                            return Mono.empty();
                        }
                        
                        log.info("Clip encontrado: {}", response.getData().get(0).getClipId());
                        return Mono.just(convertToDTO(response.getData().get(0)));
                    });
            })
            .doOnError(error -> log.error("Erro ao buscar clip {}: {}", 
                                        clipId, error.getMessage()));
    }

    public Flux<TwitchClipDTO> getClipsFromChannelExtended(String channelId, int days) {
        log.info("Buscando clips do canal {} dos últimos {} dias", channelId, days);
        
        return ensureTokenIsValid()
                .flatMapMany(token -> {
                    // Buscar clips de diferentes períodos para aumentar a chance de encontrar clips válidos
                    LocalDateTime endDate = LocalDateTime.now();
                    LocalDateTime startDate = endDate.minusDays(days);
                    
                    // Aplicar rate limiting
                    rateLimitService.acquirePermission("twitch-api", 100, Duration.ofMinutes(1));
                    
                    // Formatar as datas no formato ISO 8601 que a Twitch espera
                    String startedAtFormatted = startDate.atZone(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_INSTANT);
                    String endedAtFormatted = endDate.atZone(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_INSTANT);
                    
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/clips")
                                    .queryParam("broadcaster_id", channelId)
                                    .queryParam("started_at", startedAtFormatted)
                                    .queryParam("ended_at", endedAtFormatted)
                                    .queryParam("first", 100)
                                    .build())
                            .header("Authorization", "Bearer " + token)
                            .header("Client-Id", clientId)
                            .retrieve()
                            .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                    response -> response.bodyToMono(String.class)
                                            .flatMap(body -> {
                                                log.error("Erro na API da Twitch: {}", body);
                                                return Mono.error(new RuntimeException("Erro na API da Twitch: " + body));
                                            }))
                            .bodyToMono(TwitchClipsResponse.class)
                            .flatMapMany(response -> {
                                log.info("Encontrados {} clips no período de {} dias", 
                                        response.getData().size(), days);
                                
                                return Flux.fromIterable(response.getData());
                            })
                            .map(this::convertToDTO)
                            .doOnNext(clip -> log.debug("Clip encontrado: {} com {} visualizações", 
                                    clip.getTitle(), clip.getViewCount()));
                })
                .onErrorResume(error -> {
                    log.error("Erro ao buscar clips do canal {}: {}", channelId, error.getMessage());
                    return Flux.empty();
                });
    }

    private Mono<String> ensureTokenIsValid() {

        if(accessToken != null && tokenExpiresAt !=null && LocalDateTime.now().isBefore(tokenExpiresAt)){
            return Mono.just(accessToken);
        }


        return refreshAccessToken();
    }


    public Mono<String> refreshAccessToken() {
        return webClient.post()
                .uri("https://id.twitch.tv/oauth2/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue("client_id=" + clientId +
                        "&client_secret=" + clientSecret +
                        "&grant_type=client_credentials")
                .retrieve()
                .bodyToMono(TwitchTokenResponse.class)
                .map(response -> {
                    this.accessToken = response.getAccessToken();
                    this.tokenExpiresAt = LocalDateTime.now().plusSeconds(response.getExpiresIn() - 300); // 5 min buffer
                    log.info("Token da Twitch atualizado com sucesso");
                    return accessToken;
                })
                .doOnError(error -> log.error("Erro ao atualizar token da Twitch: {}", error.getMessage()));
    }


    private TwitchClipDTO convertToDTO(TwitchClip clip) {
        return TwitchClipDTO.builder()
                .id(clip.getClipId())
                .url(clip.getUrl())
                .embedUrl(clip.getEmbedUrl())
                .broadcasterId(clip.getBroadcasterId())
                .broadcasterName(clip.getBroadcasterName())
                .creatorId(clip.getCreatorId())
                .creatorName(clip.getCreatorName())
                .videoId(clip.getVideoId())
                .gameId(clip.getGameId())
                .gameName(clip.getGameName())
                .language(clip.getLanguage())
                .title(clip.getTitle())
                .viewCount(clip.getViewCount())
                .createdAt(clip.getCreatedAt())
                .thumbnailUrl(clip.getThumbnailUrl())
                .duration(clip.getDuration())
                .vodOffset(clip.getVodOffset())
                .build();
    }

    private TwitchUserDTO convertUserToDTO(TwitchUser twitchUser) {
        if (twitchUser == null) {
            return null;
        }
        
        TwitchUserDTO dto = new TwitchUserDTO();
        dto.setId(twitchUser.getTwitchId());
        dto.setLogin(twitchUser.getLogin());
        dto.setDisplayName(twitchUser.getDisplayName());
        dto.setType(twitchUser.getType());
        dto.setBroadcasterType(twitchUser.getBroadcasterType());
        dto.setDescription(twitchUser.getDescription());
        dto.setProfileImageUrl(twitchUser.getProfileImageUrl());
        dto.setOfflineImageUrl(twitchUser.getOfflineImageUrl());
        dto.setViewCount(twitchUser.getViewCount());
        dto.setCreatedAt(twitchUser.getCreatedAt());
        return dto;
    }

    private TwitchClipDTO convertApiToDTO(TwitchClip clip) {
        return TwitchClipDTO.builder()
                .id(clip.getClipId())
                .url(clip.getUrl())
                .embedUrl(clip.getEmbedUrl())
                .broadcasterId(clip.getBroadcasterId())
                .broadcasterName(clip.getBroadcasterName())
                .creatorId(clip.getCreatorId())
                .creatorName(clip.getCreatorName())
                .videoId(clip.getVideoId())
                .gameId(clip.getGameId())
                .gameName(clip.getGameName())
                .language(clip.getLanguage())
                .title(clip.getTitle())
                .viewCount(clip.getViewCount())
                .createdAt(clip.getCreatedAt())
                .thumbnailUrl(clip.getThumbnailUrl())
                .duration(clip.getDuration())
                .vodOffset(clip.getVodOffset())
                .build();
    }

}

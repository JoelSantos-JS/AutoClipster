package com.joel.br.AutoClipster.services;

import com.joel.br.AutoClipster.DTO.TwitchClipDTO;
import com.joel.br.AutoClipster.DTO.TwitchUserDTO;
import com.joel.br.AutoClipster.model.TwitchClip;
import com.joel.br.AutoClipster.model.TwitchUser;
import com.joel.br.AutoClipster.response.TwitchClipsResponse;
import com.joel.br.AutoClipster.response.TwitchTokenResponse;
import com.joel.br.AutoClipster.response.TwitchUsersResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
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
                            .map(response -> response.getData().get(0))
                            .map(this::convertUserToDTO);
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
                .id(clip.getId())
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

    private TwitchUserDTO  convertUserToDTO(TwitchUser twitchUser) {

        if (twitchUser == null) {
            return null; // Ou lançar uma exceção, dependendo do seu tratamento de erro
        }
        TwitchUserDTO dto = new TwitchUserDTO();
        dto.setId(twitchUser.getId());
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

}

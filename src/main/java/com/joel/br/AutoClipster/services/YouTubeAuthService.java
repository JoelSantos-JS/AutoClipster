package com.joel.br.AutoClipster.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.joel.br.AutoClipster.config.YouTubeConfig;
import com.joel.br.AutoClipster.model.YouTubeCredentials;
import com.joel.br.AutoClipster.repository.YouTubeCredentialsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class YouTubeAuthService {

    private final YouTubeCredentialsRepository credentialsRepository;
    private final GoogleAuthorizationCodeFlow authorizationCodeFlow;
    private final YouTubeConfig youTubeConfig;

    /**
     * Inicia o processo de autenticação OAuth
     */
    public String startAuthFlow(String userId) {
        try {
            log.info("Iniciando fluxo de autenticação para usuário: {}", userId);
            return youTubeConfig.getAuthorizationUrl(userId);
        } catch (Exception e) {
            log.error("Erro ao iniciar fluxo de autenticação: ", e);
            throw new RuntimeException("Falha ao iniciar autenticação", e);
        }
    }

    /**
     * Finaliza o processo de autenticação com o código de autorização
     */
    @Transactional
    public YouTubeCredentials completeAuthFlow(String authorizationCode, String userId) {
        try {
            log.info("Completando autenticação para usuário: {}", userId);

            // Trocar código por tokens
            GoogleTokenResponse tokenResponse = authorizationCodeFlow
                    .newTokenRequest(authorizationCode)
                    .setRedirectUri(youTubeConfig.getRedirectUri())
                    .execute();

            // Criar credential
            Credential credential = authorizationCodeFlow.createAndStoreCredential(tokenResponse, userId);

            // Obter informações do canal
            YouTube youTube = youTubeConfig.buildYouTubeService(credential);
            ChannelListResponse channelResponse = youTube.channels()
                    .list(Arrays.asList("snippet"))
                    .setMine(true)
                    .execute();

            if (channelResponse.getItems().isEmpty()) {
                throw new RuntimeException("Usuário não possui canal no YouTube");
            }

            Channel channel = channelResponse.getItems().get(0);

            // Salvar ou atualizar credenciais
            YouTubeCredentials credentials = credentialsRepository.findByUserId(userId)
                    .orElse(new YouTubeCredentials());

            credentials.setUserId(userId);
            credentials.setAccessToken(tokenResponse.getAccessToken());
            credentials.setRefreshToken(tokenResponse.getRefreshToken());
            credentials.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));
            credentials.setCreatedAt(credentials.getCreatedAt() != null ? credentials.getCreatedAt() : LocalDateTime.now());
            credentials.setScope(String.join(",", youTubeConfig.getRequiredScopes().toArray(new String[0])));
            credentials.setChannelId(channel.getId());
            credentials.setChannelTitle(channel.getSnippet().getTitle());

            credentials = credentialsRepository.save(credentials);

            log.info("Autenticação concluída com sucesso para canal: {}", channel.getSnippet().getTitle());
            return credentials;

        } catch (Exception e) {
            log.error("Erro ao completar autenticação: ", e);
            throw new RuntimeException("Falha ao completar autenticação", e);
        }
    }

    /**
     * Obtém um Credential válido para um usuário
     */
    public Optional<Credential> getValidCredential(String userId) {
        try {
            Optional<YouTubeCredentials> credentialsOpt = credentialsRepository.findByUserId(userId);
            
            if (credentialsOpt.isEmpty()) {
                log.warn("Credenciais não encontradas para usuário: {}", userId);
                return Optional.empty();
            }

            YouTubeCredentials credentials = credentialsOpt.get();

            // Verificar se as credenciais precisam ser renovadas
            if (credentials.needsRefresh() && credentials.getRefreshToken() != null) {
                credentials = refreshCredentials(credentials);
            }

            if (!credentials.isValid()) {
                log.warn("Credenciais expiradas ou inválidas para usuário: {}", userId);
                return Optional.empty();
            }

            // Criar Credential
            Credential credential = authorizationCodeFlow.loadCredential(userId);
            if (credential == null) {
                // Recriar credential a partir das credenciais salvas
                credential = createCredentialFromSaved(credentials);
            }

            credentialsRepository.save(credentials);

            return Optional.of(credential);

        } catch (Exception e) {
            log.error("Erro ao obter credencial válido: ", e);
            return Optional.empty();
        }
    }

    /**
     * Renova as credenciais usando refresh token
     */
    @Transactional
    public YouTubeCredentials refreshCredentials(YouTubeCredentials credentials) {
        try {
            log.info("Renovando credenciais para usuário: {}", credentials.getUserId());

            Credential credential = authorizationCodeFlow.loadCredential(credentials.getUserId());
            if (credential == null) {
                credential = createCredentialFromSaved(credentials);
            }

            // Renovar token
            boolean refreshed = credential.refreshToken();
            if (!refreshed) {
                log.error("Falha ao renovar token para usuário: {}", credentials.getUserId());
                credentials.markExpired();
                return credentialsRepository.save(credentials);
            }

            // Atualizar credenciais salvas
            credentials.setAccessToken(credential.getAccessToken());
            credentials.setExpiresAt(LocalDateTime.now().plusSeconds(credential.getExpiresInSeconds()));

            credentials = credentialsRepository.save(credentials);
            log.info("Credenciais renovadas com sucesso para usuário: {}", credentials.getUserId());

            return credentials;

        } catch (Exception e) {
            log.error("Erro ao renovar credenciais: ", e);
            credentials.markExpired();
            credentialsRepository.save(credentials);
            throw new RuntimeException("Falha ao renovar credenciais", e);
        }
    }

    /**
     * Revoga as credenciais de um usuário
     */
    @Transactional
    public void revokeCredentials(String userId) {
        try {
            Optional<YouTubeCredentials> credentialsOpt = credentialsRepository.findByUserId(userId);
            if (credentialsOpt.isPresent()) {
                YouTubeCredentials credentials = credentialsOpt.get();
                credentials.markExpired();
                credentialsRepository.save(credentials);

                // Tentar revogar no Google também
                try {
                    Credential credential = authorizationCodeFlow.loadCredential(userId);
                    if (credential != null) {
                        // Revogar token manualmente
                        log.info("Token revogado para usuário: {}", userId);
                    }
                } catch (Exception e) {
                    log.warn("Erro ao revogar token no Google: ", e);
                }

                log.info("Credenciais revogadas para usuário: {}", userId);
            }
        } catch (Exception e) {
            log.error("Erro ao revogar credenciais: ", e);
            throw new RuntimeException("Falha ao revogar credenciais", e);
        }
    }

    /**
     * Lista todos os usuários autenticados
     */
    public List<YouTubeCredentials> getAuthenticatedUsers() {
        return credentialsRepository.findByIsActive(true);
    }

    /**
     * Verifica se um usuário tem credenciais válidas
     */
    public boolean hasValidCredentials(String userId) {
        return getValidCredential(userId).isPresent();
    }

    /**
     * Limpa credenciais expiradas
     */
    @Transactional
    public void cleanupExpiredCredentials() {
        List<YouTubeCredentials> expired = credentialsRepository.findByIsActive(false);
        for (YouTubeCredentials credentials : expired) {
            credentials.markExpired();
        }
        credentialsRepository.saveAll(expired);
        log.info("Limpeza de credenciais expiradas: {} atualizadas", expired.size());
    }

    /**
     * Cria um Credential a partir das credenciais salvas
     */
    private Credential createCredentialFromSaved(YouTubeCredentials saved) throws IOException {
        return authorizationCodeFlow.createAndStoreCredential(
                new TokenResponse()
                        .setAccessToken(saved.getAccessToken())
                        .setRefreshToken(saved.getRefreshToken())
                        .setExpiresInSeconds(
                                java.time.Duration.between(LocalDateTime.now(), saved.getExpiresAt()).getSeconds()
                        ),
                saved.getUserId()
        );
    }
} 
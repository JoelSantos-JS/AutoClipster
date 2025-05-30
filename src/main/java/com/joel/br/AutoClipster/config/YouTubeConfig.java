package com.joel.br.AutoClipster.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
public class YouTubeConfig {

    @Value("${youtube.client-id}")
    private String clientId;

    @Value("${youtube.client-secret}")
    private String clientSecret;

    @Value("${youtube.redirect-uri}")
    private String redirectUri;

    private static final String APPLICATION_NAME = "AutoClipster";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(
            YouTubeScopes.YOUTUBE_UPLOAD,
            YouTubeScopes.YOUTUBE_READONLY,
            YouTubeScopes.YOUTUBEPARTNER
    );

    @Bean
    public HttpTransport httpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }

    @Bean
    public JsonFactory jsonFactory() {
        return JSON_FACTORY;
    }

    @Bean
    public GoogleClientSecrets googleClientSecrets() {
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(clientId);
        details.setClientSecret(clientSecret);
        details.setRedirectUris(Arrays.asList(redirectUri));

        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setInstalled(details);

        return clientSecrets;
    }

    @Bean
    public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow(
            HttpTransport httpTransport,
            JsonFactory jsonFactory,
            GoogleClientSecrets clientSecrets) throws IOException {

        // Diretório para armazenar tokens
        File credentialsDir = new File(System.getProperty("user.home"), ".autoclipster/youtube");
        if (!credentialsDir.exists()) {
            credentialsDir.mkdirs();
        }

        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                clientSecrets,
                SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(credentialsDir))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
    }

    public YouTube buildYouTubeService(Credential credential) throws GeneralSecurityException, IOException {
        return new YouTube.Builder(httpTransport(), jsonFactory(), credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    // Configurações auxiliares
    public static class YouTubeConstants {
        public static final String GAMING_CATEGORY_ID = "20";
        public static final String DEFAULT_PRIVACY_STATUS = "unlisted";
        public static final String VIDEO_URL_TEMPLATE = "https://www.youtube.com/watch?v=%s";
        public static final String THUMBNAIL_URL_TEMPLATE = "https://img.youtube.com/vi/%s/maxresdefault.jpg";
        
        // Limites da API do YouTube
        public static final int MAX_TITLE_LENGTH = 100;
        public static final int MAX_DESCRIPTION_LENGTH = 5000;
        public static final int MAX_TAGS_COUNT = 500;
        public static final long MAX_FILE_SIZE_BYTES = 128L * 1024 * 1024 * 1024; // 128GB
        
        // Rate limits
        public static final int UPLOADS_PER_DAY_LIMIT = 6;
        public static final int API_QUOTA_COST_UPLOAD = 1600;
        public static final int DAILY_QUOTA_LIMIT = 10000;
    }

    public List<String> getRequiredScopes() {
        return SCOPES;
    }

    public String getAuthorizationUrl(String state) {
        try {
            GoogleAuthorizationCodeFlow flow = googleAuthorizationCodeFlow(
                    httpTransport(), jsonFactory(), googleClientSecrets());
            return flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .setState(state)
                    .build();
        } catch (Exception e) {
            log.error("Erro ao gerar URL de autorização: ", e);
            throw new RuntimeException("Falha ao gerar URL de autorização", e);
        }
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.trim().isEmpty() &&
               clientSecret != null && !clientSecret.trim().isEmpty() &&
               !clientId.equals("your_youtube_client_id_here");
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setScopes(String... scopes) {
        // Este método pode ser usado para configurar escopos dinamicamente se necessário
        // Por enquanto, mantemos os escopos fixos definidos na constante SCOPES
        log.info("Escopos configurados: {}", Arrays.toString(scopes));
    }
} 
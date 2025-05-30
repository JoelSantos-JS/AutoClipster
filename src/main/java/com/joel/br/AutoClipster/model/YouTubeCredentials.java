package com.joel.br.AutoClipster.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "youtube_credentials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouTubeCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    private String userId;

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_type", length = 50)
    @Builder.Default
    private String tokenType = "Bearer";

    @Column(name = "expires_in")
    private Long expiresIn;

    @Column(name = "scope", columnDefinition = "TEXT")
    private String scope;

    @Column(name = "channel_id", length = 100)
    private String channelId;

    @Column(name = "channel_title", length = 255)
    private String channelTitle;

    @Column(name = "channel_url", length = 500)
    private String channelUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        if (expiresIn != null) {
            expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        
        if (expiresIn != null) {
            expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
        }
    }

    /**
     * Verifica se o token está expirado
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Verifica se o token expira em breve (próximos 5 minutos)
     */
    public boolean isExpiringSoon() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().plusMinutes(5).isAfter(expiresAt);
    }

    /**
     * Atualiza o timestamp de último uso
     */
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Revoga as credenciais
     */
    public void revoke() {
        this.isActive = false;
        this.accessToken = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica se o token precisa ser renovado
     */
    public boolean needsRefresh() {
        if (expiresAt == null) {
            return false; // Se não tem data de expiração, assume que não precisa renovar
        }
        // Renova se expira em menos de 5 minutos
        return LocalDateTime.now().plusMinutes(5).isAfter(expiresAt);
    }

    /**
     * Verifica se as credenciais estão válidas
     */
    public boolean isValid() {
        return accessToken != null && !accessToken.trim().isEmpty() && 
               isActive && (expiresAt == null || LocalDateTime.now().isBefore(expiresAt));
    }

    /**
     * Marca as credenciais como expiradas
     */
    public void markExpired() {
        this.isActive = false;
        this.expiresAt = LocalDateTime.now();
    }
} 
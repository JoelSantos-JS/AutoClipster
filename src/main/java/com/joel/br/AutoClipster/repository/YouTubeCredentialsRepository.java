package com.joel.br.AutoClipster.repository;

import com.joel.br.AutoClipster.model.YouTubeCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface YouTubeCredentialsRepository extends JpaRepository<YouTubeCredentials, Long> {

    /**
     * Busca credenciais por ID do usuário
     */
    Optional<YouTubeCredentials> findByUserId(String userId);

    /**
     * Busca credenciais ativas por ID do usuário
     */
    @Query("SELECT yc FROM YouTubeCredentials yc WHERE yc.userId = :userId AND yc.isActive = true")
    Optional<YouTubeCredentials> findActiveByUserId(@Param("userId") String userId);

    /**
     * Busca credenciais por canal do YouTube
     */
    Optional<YouTubeCredentials> findByChannelId(String channelId);

    /**
     * Lista todas as credenciais ativas
     */
    @Query("SELECT yc FROM YouTubeCredentials yc WHERE yc.isActive = true")
    List<YouTubeCredentials> findAllActive();

    /**
     * Lista credenciais que expiram em breve
     */
    @Query("SELECT yc FROM YouTubeCredentials yc WHERE yc.isActive = true AND yc.expiresAt <= :threshold")
    List<YouTubeCredentials> findExpiringSoon(@Param("threshold") LocalDateTime threshold);

    /**
     * Lista credenciais expiradas
     */
    @Query("SELECT yc FROM YouTubeCredentials yc WHERE yc.isActive = true AND yc.expiresAt < :now")
    List<YouTubeCredentials> findExpired(@Param("now") LocalDateTime now);

    /**
     * Lista credenciais não utilizadas há muito tempo
     */
    @Query("SELECT yc FROM YouTubeCredentials yc WHERE yc.isActive = true AND " +
           "(yc.lastUsedAt IS NULL OR yc.lastUsedAt < :threshold)")
    List<YouTubeCredentials> findUnusedSince(@Param("threshold") LocalDateTime threshold);

    /**
     * Conta credenciais ativas
     */
    @Query("SELECT COUNT(yc) FROM YouTubeCredentials yc WHERE yc.isActive = true")
    long countActive();

    /**
     * Conta credenciais por canal
     */
    long countByChannelId(String channelId);

    /**
     * Verifica se usuário tem credenciais válidas
     */
    @Query("SELECT COUNT(yc) > 0 FROM YouTubeCredentials yc WHERE yc.userId = :userId AND " +
           "yc.isActive = true AND (yc.expiresAt IS NULL OR yc.expiresAt > :now)")
    boolean hasValidCredentials(@Param("userId") String userId, @Param("now") LocalDateTime now);

    /**
     * Lista usuários autenticados únicos
     */
    @Query("SELECT DISTINCT yc.userId FROM YouTubeCredentials yc WHERE yc.isActive = true")
    List<String> findDistinctActiveUserIds();

    /**
     * Busca credenciais por refresh token
     */
    @Query("SELECT yc FROM YouTubeCredentials yc WHERE yc.refreshToken = :refreshToken AND yc.isActive = true")
    Optional<YouTubeCredentials> findByRefreshToken(@Param("refreshToken") String refreshToken);

    /**
     * Lista credenciais ordenadas por último uso
     */
    @Query("SELECT yc FROM YouTubeCredentials yc WHERE yc.isActive = true ORDER BY yc.lastUsedAt DESC")
    List<YouTubeCredentials> findAllActiveOrderByLastUsed();

    /**
     * Remove credenciais expiradas há mais de X dias
     */
    @Query("DELETE FROM YouTubeCredentials yc WHERE yc.isActive = false OR " +
           "(yc.expiresAt IS NOT NULL AND yc.expiresAt < :threshold)")
    void deleteExpiredCredentials(@Param("threshold") LocalDateTime threshold);

    /**
     * Lista credenciais por título do canal
     */
    @Query("SELECT yc FROM YouTubeCredentials yc WHERE yc.isActive = true AND " +
           "LOWER(yc.channelTitle) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<YouTubeCredentials> findByChannelTitleContainingIgnoreCase(@Param("title") String title);

    /**
     * Busca credenciais mais recentes por usuário
     */
    @Query("SELECT yc FROM YouTubeCredentials yc WHERE yc.userId = :userId " +
           "ORDER BY yc.createdAt DESC")
    List<YouTubeCredentials> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

    /**
     * Conta credenciais criadas hoje
     */
    @Query("SELECT COUNT(yc) FROM YouTubeCredentials yc WHERE yc.createdAt >= :startOfDay")
    long countCreatedToday(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Lista credenciais atualizadas recentemente
     */
    @Query("SELECT yc FROM YouTubeCredentials yc WHERE yc.isActive = true AND " +
           "yc.updatedAt >= :since ORDER BY yc.updatedAt DESC")
    List<YouTubeCredentials> findRecentlyUpdated(@Param("since") LocalDateTime since);

    /**
     * Lista credenciais por status ativo/inativo
     */
    List<YouTubeCredentials> findByIsActive(boolean isActive);
} 
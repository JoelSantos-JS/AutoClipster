package com.joel.br.AutoClipster.repository;

import com.joel.br.AutoClipster.model.YouTubeVideo;
import com.joel.br.AutoClipster.model.YouTubeVideo.UploadStatus;
import com.joel.br.AutoClipster.model.YouTubeVideo.PrivacyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface YouTubeVideoRepository extends JpaRepository<YouTubeVideo, Long> {

    /**
     * Busca vídeo por ID do YouTube
     */
    Optional<YouTubeVideo> findByYoutubeId(String youtubeId);

    /**
     * Busca vídeos por status de upload
     */
    List<YouTubeVideo> findByUploadStatus(UploadStatus uploadStatus);

    /**
     * Busca vídeos por usuário
     */
    List<YouTubeVideo> findByUserId(String userId);

    /**
     * Busca vídeos por usuário e status
     */
    List<YouTubeVideo> findByUserIdAndUploadStatus(String userId, UploadStatus uploadStatus);

    /**
     * Busca vídeo por clip
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.clip.id = :clipId")
    Optional<YouTubeVideo> findByClipId(@Param("clipId") Long clipId);

    /**
     * Busca vídeo por clip (alias para compatibilidade)
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.clip.id = :clipId")
    Optional<YouTubeVideo> findByDownloadedClipId(@Param("clipId") Long clipId);

    /**
     * Verifica se clip já foi uploadado
     */
    @Query("SELECT COUNT(yv) > 0 FROM YouTubeVideo yv WHERE yv.clip.id = :clipId AND " +
           "yv.uploadStatus IN ('COMPLETED', 'UPLOADING', 'PROCESSING')")
    boolean existsByClipId(@Param("clipId") Long clipId);

    /**
     * Lista vídeos por canal
     */
    List<YouTubeVideo> findByChannelId(String channelId);

    /**
     * Lista vídeos falhados que podem ser reprocessados
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.uploadStatus = 'FAILED' AND " +
           "yv.retryCount < yv.maxRetries ORDER BY yv.uploadStartedAt DESC")
    List<YouTubeVideo> findFailedVideosForRetry();

    /**
     * Lista vídeos em upload há muito tempo (provavelmente travados)
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.uploadStatus = 'UPLOADING' AND " +
           "yv.uploadStartedAt < :threshold")
    List<YouTubeVideo> findStuckUploads(@Param("threshold") LocalDateTime threshold);

    /**
     * Lista vídeos completados recentemente
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.uploadStatus = 'COMPLETED' AND " +
           "yv.uploadCompletedAt >= :since ORDER BY yv.uploadCompletedAt DESC")
    List<YouTubeVideo> findRecentlyCompleted(@Param("since") LocalDateTime since);

    /**
     * Conta vídeos por status
     */
    long countByUploadStatus(UploadStatus uploadStatus);

    /**
     * Conta vídeos por usuário
     */
    long countByUserId(String userId);

    /**
     * Conta vídeos por usuário e status
     */
    long countByUserIdAndUploadStatus(String userId, UploadStatus uploadStatus);

    /**
     * Lista vídeos por status de privacidade
     */
    List<YouTubeVideo> findByPrivacyStatus(PrivacyStatus privacyStatus);

    /**
     * Lista vídeos com mais visualizações
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.uploadStatus = 'COMPLETED' AND " +
           "yv.viewCount IS NOT NULL ORDER BY yv.viewCount DESC")
    List<YouTubeVideo> findTopViewedVideos();

    /**
     * Lista vídeos criados em um período
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.createdAt BETWEEN :start AND :end " +
           "ORDER BY yv.createdAt DESC")
    List<YouTubeVideo> findByCreatedAtBetween(@Param("start") LocalDateTime start, 
                                              @Param("end") LocalDateTime end);

    /**
     * Lista vídeos que precisam de atualização de estatísticas
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.uploadStatus = 'COMPLETED' AND " +
           "(yv.statsUpdatedAt IS NULL OR yv.statsUpdatedAt < :threshold)")
    List<YouTubeVideo> findNeedingStatsUpdate(@Param("threshold") LocalDateTime threshold);

    /**
     * Busca vídeos por título (busca parcial)
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE LOWER(yv.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<YouTubeVideo> findByTitleContainingIgnoreCase(@Param("title") String title);

    /**
     * Lista vídeos duplicados (mesmo clip uploadado múltiplas vezes)
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.clip.id IN " +
           "(SELECT yv2.clip.id FROM YouTubeVideo yv2 GROUP BY yv2.clip.id HAVING COUNT(yv2) > 1) " +
           "ORDER BY yv.clip.id, yv.createdAt")
    List<YouTubeVideo> findDuplicateUploads();

    /**
     * Lista vídeos por categoria
     */
    List<YouTubeVideo> findByCategoryId(String categoryId);

    /**
     * Estatísticas gerais de upload
     */
    @Query("SELECT yv.uploadStatus as status, COUNT(yv) as count FROM YouTubeVideo yv " +
           "GROUP BY yv.uploadStatus")
    List<Object[]> getUploadStatistics();

    /**
     * Lista vídeos com erro específico
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.uploadStatus = 'FAILED' AND " +
           "LOWER(yv.uploadError) LIKE LOWER(CONCAT('%', :error, '%'))")
    List<YouTubeVideo> findByUploadErrorContaining(@Param("error") String error);

    /**
     * Lista vídeos publicados em um período
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.publishedAt BETWEEN :start AND :end " +
           "ORDER BY yv.publishedAt DESC")
    List<YouTubeVideo> findByPublishedAtBetween(@Param("start") LocalDateTime start, 
                                                @Param("end") LocalDateTime end);

    /**
     * Soma total de visualizações por usuário
     */
    @Query("SELECT COALESCE(SUM(yv.viewCount), 0) FROM YouTubeVideo yv WHERE yv.userId = :userId AND " +
           "yv.uploadStatus = 'COMPLETED'")
    Long getTotalViewsByUserId(@Param("userId") String userId);

    /**
     * Lista vídeos ordenados por data de criação
     */
    @Query("SELECT yv FROM YouTubeVideo yv ORDER BY yv.createdAt DESC")
    List<YouTubeVideo> findAllOrderByCreatedAtDesc();

    /**
     * Lista últimos vídeos por usuário
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.userId = :userId " +
           "ORDER BY yv.createdAt DESC")
    List<YouTubeVideo> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

    /**
     * Conta vídeos criados hoje
     */
    @Query("SELECT COUNT(yv) FROM YouTubeVideo yv WHERE yv.createdAt >= :startOfDay")
    long countCreatedToday(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Lista vídeos com upload bem-sucedido mas sem YouTube ID (inconsistência)
     */
    @Query("SELECT yv FROM YouTubeVideo yv WHERE yv.uploadStatus = 'COMPLETED' AND " +
           "(yv.youtubeId IS NULL OR yv.youtubeId = '')")
    List<YouTubeVideo> findInconsistentVideos();
} 
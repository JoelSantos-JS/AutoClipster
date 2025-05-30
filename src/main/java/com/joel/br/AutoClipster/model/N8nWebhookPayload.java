package com.joel.br.AutoClipster.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class N8nWebhookPayload {
    
    @JsonProperty("event")
    private String event;
    
    @Builder.Default
    @JsonProperty("timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Builder.Default
    @JsonProperty("source")
    private String source = "AutoClipster";
    
    @JsonProperty("data")
    private Map<String, Object> data;
    
    @JsonProperty("clipInfo")
    private ClipInfo clipInfo;
    
    @JsonProperty("youtubeInfo")
    private YouTubeInfo youtubeInfo;
    
    @JsonProperty("geminiAnalysis")
    private GeminiAnalysis geminiAnalysis;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClipInfo {
        @JsonProperty("clipId")
        private Long clipId;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("creator")
        private String creator;
        
        @JsonProperty("game")
        private String game;
        
        @JsonProperty("duration")
        private Double duration;
        
        @JsonProperty("viewCount")
        private Integer viewCount;
        
        @JsonProperty("downloadDate")
        private LocalDateTime downloadDate;
        
        @JsonProperty("originalUrl")
        private String originalUrl;
        
        @JsonProperty("filePath")
        private String filePath;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class YouTubeInfo {
        @JsonProperty("youtube_id")
        private String youtubeId;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("upload_date")
        private LocalDateTime uploadDate;
        
        @JsonProperty("views")
        private Long views;
        
        @JsonProperty("likes")
        private Long likes;
        
        @JsonProperty("comments")
        private Long comments;
        
        @JsonProperty("url")
        private String url;
        
        public static YouTubeInfo from(YouTubeVideo video) {
            return YouTubeInfo.builder()
                    .youtubeId(video.getYoutubeId())
                    .title(video.getTitle())
                    .description(video.getDescription())
                    .status(video.getUploadStatus() != null ? video.getUploadStatus().getValue() : null)
                    .uploadDate(video.getUploadCompletedAt())
                    .views(video.getViewCount())
                    .likes(video.getLikeCount())
                    .comments(video.getCommentCount())
                    .url(video.getFullWatchUrl())
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GeminiAnalysis {
        @JsonProperty("optimizedTitle")
        private String optimizedTitle;
        
        @JsonProperty("optimizedDescription")
        private String optimizedDescription;
        
        @JsonProperty("tags")
        private String[] tags;
        
        @JsonProperty("viralScore")
        private Double viralScore;
        
        @JsonProperty("sentimentScore")
        private Double sentimentScore;
        
        @JsonProperty("analysisDate")
        private LocalDateTime analysisDate;
    }

    // Factory methods para diferentes tipos de eventos
    public static N8nWebhookPayload clipDownloaded(DownloadedClip clip) {
        N8nWebhookPayload payload = new N8nWebhookPayload();
        payload.setEvent("clip.downloaded");
        payload.setTimestamp(LocalDateTime.now());
        
        ClipInfo clipInfo = new ClipInfo();
        clipInfo.setClipId(clip.getId());
        clipInfo.setTitle(clip.getTitle());
        clipInfo.setCreator(clip.getCreatorName());
        clipInfo.setGame(clip.getGameName());
        clipInfo.setDuration(clip.getDuration());
        clipInfo.setViewCount(clip.getViewCount());
        clipInfo.setDownloadDate(clip.getDownloadDate());
        clipInfo.setOriginalUrl(clip.getOriginalUrl());
        clipInfo.setFilePath(clip.getFilePath());
        
        payload.setClipInfo(clipInfo);
        return payload;
    }

    public static N8nWebhookPayload clipAnalyzed(DownloadedClip clip, String optimizedTitle, 
                                                String optimizedDescription, String[] tags) {
        N8nWebhookPayload payload = clipDownloaded(clip);
        payload.setEvent("clip.analyzed");
        
        GeminiAnalysis analysis = new GeminiAnalysis();
        analysis.setOptimizedTitle(optimizedTitle);
        analysis.setOptimizedDescription(optimizedDescription);
        analysis.setTags(tags);
        analysis.setAnalysisDate(LocalDateTime.now());
        
        payload.setGeminiAnalysis(analysis);
        return payload;
    }

    public static N8nWebhookPayload youtubeUploaded(DownloadedClip clip, YouTubeVideo video) {
        N8nWebhookPayload payload = clipDownloaded(clip);
        payload.setEvent("youtube.uploaded");
        
        YouTubeInfo youtubeInfo = YouTubeInfo.from(video);
        
        payload.setYoutubeInfo(youtubeInfo);
        return payload;
    }

    public static N8nWebhookPayload workflowCompleted(DownloadedClip clip, YouTubeVideo video) {
        N8nWebhookPayload payload = youtubeUploaded(clip, video);
        payload.setEvent("workflow.completed");
        return payload;
    }

    public static N8nWebhookPayload error(String errorType, String errorMessage, Map<String, Object> context) {
        N8nWebhookPayload payload = new N8nWebhookPayload();
        payload.setEvent("error.occurred");
        payload.setTimestamp(LocalDateTime.now());
        
        payload.getData().put("errorType", errorType);
        payload.getData().put("errorMessage", errorMessage);
        payload.getData().put("context", context);
        
        return payload;
    }
} 
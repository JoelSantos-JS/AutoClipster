package com.joel.br.AutoClipster.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TwitchClipDTO {

    private String id; // Clip ID from Twitch
    private String url;
    private String embedUrl;
    private String broadcasterId;
    private String broadcasterName;
    private String creatorId;
    private String creatorName;
    private String videoId; // ID of the VOD, if from a VOD
    private String gameId;
    private String gameName;
    private String language;
    private String title;
    private Integer viewCount;
    private LocalDateTime createdAt; // Twitch's creation date for the clip
    private String thumbnailUrl;
    private Double duration; // Duration in seconds (Twitch API provides this as double or string usually)
    private Integer vodOffset; // If the clip is from a VOD, the offset in seconds
    private Double viralScore; // Added by ClipFilterService
    // Potentially other fields if needed for internal processing before becoming TwitchClipInfo entity
}

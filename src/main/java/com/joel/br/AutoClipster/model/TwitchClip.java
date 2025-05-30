package com.joel.br.AutoClipster.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;


@Entity
@Data
@Table
@AllArgsConstructor
@NoArgsConstructor
public class TwitchClip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;
    
    @JsonProperty("id")
    @Column(unique = true, nullable = false)
    private String clipId;
    
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
    @JsonProperty("view_count")
    private Integer viewCount;
    private LocalDateTime createdAt = LocalDateTime.now();// Twitch's creation date for the clip
    private String thumbnailUrl;
    private Double duration; // Duration in seconds (Twitch API provides this as double or string usually)
    private Integer vodOffset; // If the clip is from a VOD, the offset in seconds
    private Double viralScore; // Added by ClipFil
}

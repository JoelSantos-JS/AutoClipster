package com.joel.br.AutoClipster.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwitchUser {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    private String login;

    @JsonProperty("display_name")
    private String displayName;

    private String type;

    @JsonProperty("broadcaster_type")
    private String broadcasterType;

    private String description;

    @JsonProperty("profile_image_url")
    private String profileImageUrl;

    @JsonProperty("offline_image_url")
    private String offlineImageUrl;

    @JsonProperty("view_count")
    private long viewCount; // API do Twitch geralmente retorna como número

    @JsonProperty("created_at")
    private String createdAt; // API do Twitch retorna como String (ISO 8601)
}

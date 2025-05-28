package com.joel.br.AutoClipster.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwitchUserDTO {

    private String id;
    private String login;
    private String displayName;
    private String type;
    private String broadcasterType;
    private String description;
    private String profileImageUrl;
    private String offlineImageUrl;
    private Long viewCount; // Ou int, dependendo da sua preferência
    private String createdAt; // Ou Loca
}

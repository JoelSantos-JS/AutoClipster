package com.joel.br.AutoClipster.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwitchTokenResponse {

    @JsonProperty("access_token") // Mapeia o campo JSON "access_token" para o campo Java accessToken
    private String accessToken;

    @JsonProperty("expires_in")   // Mapeia o campo JSON "expires_in" para o campo Java expiresIn
    private int expiresIn;

    @JsonProperty("token_type")   // Mapeia o campo JSON "token_type" para o campo Java tokenType
    private String tokenType;
}

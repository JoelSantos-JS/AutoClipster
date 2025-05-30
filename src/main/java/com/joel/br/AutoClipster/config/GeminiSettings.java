package com.joel.br.AutoClipster.config;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class GeminiSettings {
private final float temperature;
private final float topP;
private final int topK;
private final int maxOutputTokens;
private final String modelName;
}
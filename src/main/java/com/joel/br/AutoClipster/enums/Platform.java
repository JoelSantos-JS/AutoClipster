package com.joel.br.AutoClipster.enums;

public enum Platform {

    TIKTOK("TikTok"),
    YOUTUBE("YouTube"),
    INSTAGRAM("Instagram");

    private final String displayName;

    Platform(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

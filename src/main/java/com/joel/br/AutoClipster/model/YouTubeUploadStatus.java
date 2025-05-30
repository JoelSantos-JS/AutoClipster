package com.joel.br.AutoClipster.model;

/**
 * Enum para status de upload do YouTube
 */
public enum YouTubeUploadStatus {
    PENDING("pending"),
    UPLOADING("uploading"), 
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed"),
    REJECTED("rejected"),
    DUPLICATE("duplicate");

    private final String value;

    YouTubeUploadStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static YouTubeUploadStatus fromValue(String value) {
        for (YouTubeUploadStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return PENDING; // Default
    }

    @Override
    public String toString() {
        return value;
    }
} 
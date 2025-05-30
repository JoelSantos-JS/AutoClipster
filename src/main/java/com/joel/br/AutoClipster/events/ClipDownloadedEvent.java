package com.joel.br.AutoClipster.events;

import com.joel.br.AutoClipster.model.DownloadedClip;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento disparado quando um clip é baixado com sucesso
 * Usado para iniciar o processamento automático
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClipDownloadedEvent {
    private DownloadedClip downloadedClip;
    private LocalDateTime downloadedAt;
    private String source; // "MANUAL", "AUTOMATED", "SCHEDULED"
} 
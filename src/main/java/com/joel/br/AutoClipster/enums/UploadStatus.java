package com.joel.br.AutoClipster.enums;

public enum UploadStatus {
    PENDING,         // Aguardando upload
    UPLOADING,       // Upload em andamento
    COMPLETED,       // Upload realizado com sucesso
    FAILED,          // Falha no upload
    SCHEDULED,       // Agendado para upload futuro
    RATE_LIMITED,    // Limitado por rate limit
    RETRY_SCHEDULED  // Agendado para retry
}

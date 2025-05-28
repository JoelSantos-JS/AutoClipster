package com.joel.br.AutoClipster.enums;

public enum ProcessingStatus {
    DISCOVERED,      // Clipe descoberto
    ANALYZING,       // Análise de IA em andamento
    DOWNLOADING,     // Download do vídeo
    PROCESSING,      // Processamento do vídeo
    GENERATING_CONTENT, // Geração de conteúdo com IA
    READY_FOR_UPLOAD,   // Pronto para upload
    COMPLETED,       // Processamento completo
    FAILED,          // Falha no processamento
    SKIPPED          // Clipe ignorado (baixo score viral)

}

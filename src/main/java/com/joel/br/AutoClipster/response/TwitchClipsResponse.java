package com.joel.br.AutoClipster.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.joel.br.AutoClipster.model.TwitchClip;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Classe que mapeia a resposta da API da Twitch para consultas de clipes.
 * A estrutura segue o formato padrão da API Helix da Twitch.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwitchClipsResponse {
    
    /**
     * Lista de clipes retornados pela API
     */
    private List<TwitchClip> data;
    
    /**
     * Informações de paginação para consultas subsequentes
     */
    private Pagination pagination;
    
    /**
     * Classe interna que representa as informações de paginação
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        /**
         * Cursor para a próxima página de resultados
         */
        private String cursor;
    }
}

package com.joel.br.AutoClipster.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClipMetaData {


    @JsonProperty("optimizedTitle")
    private String optimizedTitle;

    @JsonProperty("youtubeDescription")
    private String youtubeDescription;


    @JsonProperty("tags")
    private List<String> tags;


    @JsonProperty("thumbnailSuggestion")
    private String thumbnailSuggestion;

    @JsonProperty("estimatedViews")
    private Integer estimatedViews;

    @JsonProperty("bestUploadTime")
    private String bestUploadTime;

    @JsonProperty("category")
    private String category;
    
    @JsonProperty("socialHashtags")
    private List<String> socialHashtags;
}

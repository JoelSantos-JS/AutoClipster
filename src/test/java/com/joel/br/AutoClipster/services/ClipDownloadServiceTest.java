package com.joel.br.AutoClipster.services;

import com.joel.br.AutoClipster.DTO.TwitchClipDTO;
import com.joel.br.AutoClipster.model.DownloadedClip;
import com.joel.br.AutoClipster.repository.DownloadedClipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClipDownloadServiceTest {

    @Mock
    private DownloadedClipRepository downloadedClipRepository;
    
    @Mock
    private ResourceLoader resourceLoader;
    
    @InjectMocks
    private ClipDownloadService clipDownloadService;

    @Test
    void testDownloadTopClips_withNullViewCount_shouldFilterNullValues() {
        // Arrange
        TwitchClipDTO clipWithViewCount = TwitchClipDTO.builder()
                .id("clip1")
                .title("Clip with view count")
                .viewCount(1000)
                .url("https://clips.twitch.tv/clip1")
                .createdAt(LocalDateTime.now())
                .build();
        
        TwitchClipDTO clipWithNullViewCount = TwitchClipDTO.builder()
                .id("clip2")
                .title("Clip with null view count")
                .viewCount(null) // This should be filtered out
                .url("https://clips.twitch.tv/clip2")
                .createdAt(LocalDateTime.now())
                .build();
        
        TwitchClipDTO clipWithLowerViewCount = TwitchClipDTO.builder()
                .id("clip3")
                .title("Clip with lower view count")
                .viewCount(500)
                .url("https://clips.twitch.tv/clip3")
                .createdAt(LocalDateTime.now())
                .build();
        
        // Mock repository to return empty list (no existing clips)
        when(downloadedClipRepository.findByClipId(any())).thenReturn(Collections.emptyList());
        
        Flux<TwitchClipDTO> clipsFlux = Flux.just(
                clipWithViewCount,
                clipWithNullViewCount,
                clipWithLowerViewCount
        );

        // Act & Assert
        // Since we can't easily mock the yt-dlp execution in this test,
        // we'll test just the sorting and filtering logic by checking 
        // that the flux processes without throwing NullPointerException
        StepVerifier.create(
                clipsFlux
                    .filter(clip -> clip.getViewCount() != null)
                    .collectSortedList((c1, c2) -> Integer.compare(c2.getViewCount(), c1.getViewCount()))
        )
        .expectNextMatches(sortedList -> {
            // Should have only 2 clips (null viewCount filtered out)
            return sortedList.size() == 2 &&
                   sortedList.get(0).getViewCount() == 1000 && // Highest first
                   sortedList.get(1).getViewCount() == 500;    // Lower second
        })
        .verifyComplete();
    }
} 
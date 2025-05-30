// Serviço para baixar clips da Twitch
package com.joel.br.AutoClipster.services;

import com.joel.br.AutoClipster.DTO.TwitchClipDTO;
import com.joel.br.AutoClipster.model.DownloadedClip;
import com.joel.br.AutoClipster.repository.DownloadedClipRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ClipDownloadService {

    private final DownloadedClipRepository downloadedClipRepository;
    private final ResourceLoader resourceLoader;
    
    @Value("${app.clips.download-path:./downloads}")
    private String downloadPath;
    
    @Value("${app.download.timeout:300}")
    private int downloadTimeout;
    
    private String ytDlpPath;

    public ClipDownloadService(DownloadedClipRepository downloadedClipRepository, ResourceLoader resourceLoader) {
        this.downloadedClipRepository = downloadedClipRepository;
        this.resourceLoader = resourceLoader;
    }
    
    @PostConstruct
    public void initialize() {
        // Garantir que o diretório de download existe
        try {
            Files.createDirectories(Paths.get(downloadPath));
            setupYtDlp();
            log.info("ClipDownloadService inicializado com sucesso. Download path: {}", downloadPath);
        } catch (IOException e) {
            log.error("Erro ao inicializar ClipDownloadService: {}", e.getMessage());
        }
    }
    
    /**
     * Configura o yt-dlp.exe, copiando-o do classpath para um local temporário se necessário
     */
    private void setupYtDlp() throws IOException {
        // Tentar usar o yt-dlp.exe do classpath
        Resource ytDlpResource = resourceLoader.getResource("classpath:yt-dlp.exe");
        if (ytDlpResource.exists()) {
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "autoclipster");
            tempDir.mkdirs();
            
            File ytDlpFile = new File(tempDir, "yt-dlp.exe");
            Files.copy(ytDlpResource.getInputStream(), ytDlpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            ytDlpFile.setExecutable(true);
            
            ytDlpPath = ytDlpFile.getAbsolutePath();
            log.info("yt-dlp.exe configurado em: {}", ytDlpPath);
        } else {
            // Fallback para 'yt-dlp' no PATH
            ytDlpPath = "yt-dlp";
            log.warn("yt-dlp.exe não encontrado no classpath, usando comando 'yt-dlp' do PATH");
        }
    }
    
    /**
     * Baixa os N primeiros clipes, ordenados por visualizações
     */
    public Mono<Integer> downloadTopClips(Flux<TwitchClipDTO> clipsFlux, int limit) {
        log.info("Iniciando download dos {} melhores clips", limit);

        return clipsFlux
                .collectList()
                .flatMap(clips -> {
                    log.info("Total de clips recebidos: {}", clips.size());
                    
                    if (clips.isEmpty()) {
                        log.warn("Nenhum clip foi fornecido para download");
                        return Mono.just(0);
                    }

                    // Log detalhado dos clips recebidos
                    clips.forEach(clip -> {
                        if (clip.getViewCount() == null) {
                            log.warn("Clip '{}' tem viewCount null", clip.getTitle());
                        } else {
                            log.debug("Clip '{}' tem {} visualizações", clip.getTitle(), clip.getViewCount());
                        }
                    });

                    // Buscar clips já baixados de forma síncrona
                    List<DownloadedClip> existingClips = downloadedClipRepository.findAll();
                    List<String> existingUrls = existingClips.stream()
                            .map(DownloadedClip::getOriginalUrl)
                            .toList();

                    // Filtrar clips que ainda não foram baixados
                    List<TwitchClipDTO> newClips = clips.stream()
                            .filter(clip -> !existingUrls.contains(clip.getUrl()))
                            .toList();

                    log.info("Clips não baixados: {}", newClips.size());

                    if (newClips.isEmpty()) {
                        log.info("Todos os clips já foram baixados anteriormente");
                        return Mono.just(0);
                    }

                    // Separar clips com e sem viewCount
                    List<TwitchClipDTO> clipsWithViewCount = newClips.stream()
                            .filter(clip -> clip.getViewCount() != null)
                            .toList();
                    
                    List<TwitchClipDTO> clipsWithoutViewCount = newClips.stream()
                            .filter(clip -> clip.getViewCount() == null)
                            .toList();

                    log.info("Clips com viewCount válido: {}", clipsWithViewCount.size());
                    log.info("Clips com viewCount null: {}", clipsWithoutViewCount.size());

                    List<TwitchClipDTO> sortedClips;
                    
                    if (!clipsWithViewCount.isEmpty()) {
                        // Priorizar clips com viewCount válido
                        sortedClips = clipsWithViewCount.stream()
                                .sorted((c1, c2) -> Integer.compare(c2.getViewCount(), c1.getViewCount()))
                                .limit(limit)
                                .toList();
                        
                        log.info("Usando {} clips com viewCount válido para download", sortedClips.size());
                    } else {
                        // Se todos têm viewCount null, usar ordenação por data (mais recentes primeiro)
                        log.warn("Todos os clips têm viewCount null, ordenando por data de criação");
                        sortedClips = clipsWithoutViewCount.stream()
                                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                                .limit(limit)
                                .toList();
                    }

                    if (sortedClips.isEmpty()) {
                        log.warn("Nenhum clip disponível para download após filtragem");
                        return Mono.just(0);
                    }

                    log.info("Iniciando download de {} clips", sortedClips.size());

                    // Baixar clips
                    int downloadedCount = 0;
                    for (TwitchClipDTO clip : sortedClips) {
                        try {
                            log.info("Baixando clip: '{}' (URL: {})", clip.getTitle(), clip.getUrl());
                            downloadClip(clip);
                            downloadedCount++;
                            log.info("Clip baixado com sucesso: '{}'", clip.getTitle());
                        } catch (Exception e) {
                            log.error("Erro ao baixar clip '{}': {}", clip.getTitle(), e.getMessage());
                        }
                    }

                    log.info("Download concluído para {} clips", downloadedCount);
                    return Mono.just(downloadedCount);
                })
                .onErrorResume(error -> {
                    log.error("Erro durante o processo de download: {}", error.getMessage(), error);
                    return Mono.just(0);
                });
    }

    /**
     * Baixa um único clip da Twitch usando yt-dlp
     */
    public void downloadClip(TwitchClipDTO clip) {
        log.info("Iniciando download do clip: {} ({})", clip.getTitle(), clip.getUrl());
        
        // Verificar se o clip já existe
        List<DownloadedClip> existingClips = downloadedClipRepository.findByClipId(clip.getId());
        if (!existingClips.isEmpty()) {
            log.info("Clip já baixado anteriormente: {}", clip.getId());
            return;
        }
        
        // Gerar um nome de arquivo único baseado no ID do clip
        String outputFileName = sanitizeFileName(clip.getTitle()) + "_" + clip.getId() + ".mp4";
        Path outputPath = Paths.get(downloadPath, outputFileName);
        
        boolean successful = downloadUsingYtDlpSync(clip.getUrl(), outputPath.toString());
        
        if (successful) {
            // Criar e salvar o objeto DownloadedClip
            DownloadedClip downloadedClip = new DownloadedClip();
            downloadedClip.setClipId(clip.getId());
            downloadedClip.setTitle(clip.getTitle());
            downloadedClip.setViewCount(clip.getViewCount());
            downloadedClip.setCreatorName(clip.getCreatorName());
            downloadedClip.setBroadcasterName(clip.getBroadcasterName());
            downloadedClip.setDownloadDate(LocalDateTime.now());
            downloadedClip.setFilePath(outputPath.toString());
            downloadedClip.setGameName(clip.getGameName());
            downloadedClip.setDuration(clip.getDuration());
            downloadedClip.setOriginalUrl(clip.getUrl());
            downloadedClip.setProcessed(false);
            
            downloadedClipRepository.save(downloadedClip);
            log.info("Clip salvo no banco de dados: {}", clip.getTitle());
        } else {
            throw new RuntimeException("Falha ao baixar clip: " + clip.getUrl());
        }
    }
    
    /**
     * Sanitiza o nome do arquivo removendo caracteres inválidos
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_").substring(0, Math.min(fileName.length(), 50));
    }

    /**
     * Executa yt-dlp para baixar o clip (versão síncrona)
     */
    private boolean downloadUsingYtDlpSync(String clipUrl, String outputPath) {
        try {
            log.info("Executando yt-dlp para baixar: {} para {}", clipUrl, outputPath);
            
            // Preparar o comando
            ProcessBuilder pb = new ProcessBuilder(
                ytDlpPath,
                "--format", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best", // Melhor qualidade possível com preferência por MP4
                "-o", outputPath,   // Caminho de saída
                "--no-playlist",    // Não baixar playlists
                clipUrl             // URL do clip
            );
            
            // Redirecionar erro para saída padrão
            pb.redirectErrorStream(true);
            
            // Iniciar o processo
            Process process = pb.start();
            
            // Ler a saída do processo
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("yt-dlp output: {}", line);
                }
            }
            
            // Aguardar a conclusão com timeout
            boolean completed = process.waitFor(downloadTimeout, TimeUnit.SECONDS);
            if (!completed) {
                log.error("Timeout ao baixar clip: {}", clipUrl);
                process.destroy();
                return false;
            }
            
            // Verificar código de saída
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("yt-dlp falhou com código de saída {}: {}", exitCode, clipUrl);
                return false;
            }
            
            // Verificar se o arquivo foi criado
            File outputFile = new File(outputPath);
            if (!outputFile.exists() || outputFile.length() == 0) {
                log.error("Arquivo de saída não encontrado ou vazio: {}", outputPath);
                return false;
            }
            
            log.info("Download concluído com sucesso: {}", outputPath);
            return true;
        } catch (Exception e) {
            log.error("Erro ao executar yt-dlp: {}", e.getMessage(), e);
            return false;
        }
    }
}
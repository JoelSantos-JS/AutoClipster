package com.joel.br.AutoClipster.repository;

import com.joel.br.AutoClipster.model.DownloadedClip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DownloadedClipRepository extends JpaRepository<DownloadedClip, Long> {
    List<DownloadedClip> findByProcessedFalse();
    List<DownloadedClip> findByClipId(String clipId);
}

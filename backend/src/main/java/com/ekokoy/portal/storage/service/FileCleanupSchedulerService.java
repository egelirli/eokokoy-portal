package com.ekokoy.portal.storage.service;

import com.ekokoy.portal.storage.entity.StoredFile;
import com.ekokoy.portal.storage.repository.StoredFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FileCleanupSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupSchedulerService.class);

    private final StoredFileRepository fileRepository;

    public FileCleanupSchedulerService(StoredFileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    /**
     * Onaylanmamış ve 24 saatten eski dosya kayıtlarını soft-delete yapar.
     * MinIO'dan fiziksel silme yapılmaz (spec gereği).
     * Her saat çalışır.
     */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void cleanupUnconfirmedFiles() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        List<StoredFile> stale = fileRepository.findUnconfirmedBefore(cutoff);
        if (stale.isEmpty()) return;

        Instant now = Instant.now();
        for (StoredFile file : stale) {
            file.setDeleted(true);
            file.setDeletedAt(now);
        }
        fileRepository.saveAll(stale);
        log.info("Temizlik: {} onaylanmamış dosya soft-delete yapıldı.", stale.size());
    }
}

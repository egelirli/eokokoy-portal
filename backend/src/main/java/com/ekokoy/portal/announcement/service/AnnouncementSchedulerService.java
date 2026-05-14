package com.ekokoy.portal.announcement.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Zamanlanmış duyuru işlemlerini yönetir.
 * Her dakika çalışarak:
 *  - scheduledAt zamanı gelen draft duyuruları yayına alır
 *  - expiresAt zamanı geçen yayınlanmış duyuruları arşivler
 */
@Service
public class AnnouncementSchedulerService {

    private final AnnouncementService announcementService;

    public AnnouncementSchedulerService(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void processScheduledAnnouncements() {
        announcementService.publishScheduledAnnouncements();
        announcementService.archiveExpiredAnnouncements();
    }
}

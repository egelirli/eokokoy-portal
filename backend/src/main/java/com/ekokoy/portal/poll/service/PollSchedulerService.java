package com.ekokoy.portal.poll.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Zamanlanmış anket işlemlerini yönetir.
 * Her dakika çalışarak:
 *  - starts_at zamanı gelen draft anketleri aktifleştirir
 *  - ends_at zamanı geçen aktif anketleri kapatır
 */
@Service
public class PollSchedulerService {

    private final PollService pollService;

    public PollSchedulerService(PollService pollService) {
        this.pollService = pollService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void processPolls() {
        pollService.activateScheduledPolls();
        pollService.closeExpiredPolls();
    }
}

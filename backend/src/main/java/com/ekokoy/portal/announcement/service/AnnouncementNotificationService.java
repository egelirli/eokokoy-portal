package com.ekokoy.portal.announcement.service;

import com.ekokoy.portal.announcement.entity.Announcement;
import com.ekokoy.portal.announcement.entity.AnnouncementPriority;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserStatus;
import com.ekokoy.portal.user.repository.UserRepository;
import com.ekokoy.portal.user.service.EmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Duyuru yayın bildirimlerini yönetir.
 * urgent öncelikli duyurular tüm kanallardan gider (kullanıcı tercihi atlanır).
 * WhatsApp kanalı SPEC-10 tamamlandığında genişletilecek.
 */
@Service
public class AnnouncementNotificationService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public AnnouncementNotificationService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /** Duyuru yayınlandığında ilgili kullanıcılara bildirim gönderir. */
    @Async
    public void notifyPublished(Announcement announcement) {
        if (announcement.getPriority() == AnnouncementPriority.urgent) {
            sendUrgentNotifications(announcement);
        }
        // normal/important için SPEC-10 bildirim modülü devreye girecek
    }

    private void sendUrgentNotifications(Announcement announcement) {
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(u -> !u.isDeleted() && u.getStatus() == UserStatus.active)
                .toList();

        for (User user : activeUsers) {
            emailService.sendAnnouncementEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    announcement.getTitle(),
                    announcement.getBody()
            );
        }
    }
}

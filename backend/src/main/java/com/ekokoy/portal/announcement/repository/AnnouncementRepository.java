package com.ekokoy.portal.announcement.repository;

import com.ekokoy.portal.announcement.entity.Announcement;
import com.ekokoy.portal.announcement.entity.AnnouncementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    /** Yayınlanmış, herkese açık duyuruları döner. */
    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.attachments WHERE a.status = 'published' AND a.isPublic = true ORDER BY a.publishedAt DESC")
    List<Announcement> findPublishedPublic();

    /** Yayınlanmış tüm duyuruları döner (authenticated kullanıcılar için). */
    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.attachments WHERE a.status = 'published' ORDER BY a.publishedAt DESC")
    List<Announcement> findAllPublished();

    /** Bağlı nesnelerle birlikte tek duyuru getirir. */
    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.attachments LEFT JOIN FETCH a.targets LEFT JOIN FETCH a.createdBy WHERE a.id = :id")
    Optional<Announcement> findByIdWithDetails(UUID id);

    /** Scheduler: zamanlanmış yayın zamanı gelmiş draft duyurular. */
    @Query("SELECT a FROM Announcement a WHERE a.status = 'draft' AND a.scheduledAt IS NOT NULL AND a.scheduledAt <= :now")
    List<Announcement> findDraftsDueForPublishing(Instant now);

    /** Scheduler: süresi dolmuş yayınlanmış duyurular. */
    @Query("SELECT a FROM Announcement a WHERE a.status = 'published' AND a.expiresAt IS NOT NULL AND a.expiresAt <= :now")
    List<Announcement> findExpiredPublished(Instant now);
}

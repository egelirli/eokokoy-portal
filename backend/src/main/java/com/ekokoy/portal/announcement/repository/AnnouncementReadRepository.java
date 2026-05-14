package com.ekokoy.portal.announcement.repository;

import com.ekokoy.portal.announcement.entity.AnnouncementRead;
import com.ekokoy.portal.announcement.entity.AnnouncementReadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, AnnouncementReadId> {

    boolean existsByAnnouncementIdAndUserId(UUID announcementId, UUID userId);

    @Query("SELECT ar FROM AnnouncementRead ar JOIN FETCH ar.user u WHERE ar.announcement.id = :announcementId ORDER BY ar.readAt DESC")
    List<AnnouncementRead> findByAnnouncementIdWithUser(UUID announcementId);

    long countByAnnouncementId(UUID announcementId);
}

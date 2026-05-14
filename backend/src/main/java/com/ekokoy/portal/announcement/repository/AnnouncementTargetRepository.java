package com.ekokoy.portal.announcement.repository;

import com.ekokoy.portal.announcement.entity.AnnouncementTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnouncementTargetRepository extends JpaRepository<AnnouncementTarget, UUID> {

    List<AnnouncementTarget> findByAnnouncementId(UUID announcementId);

    void deleteByAnnouncementId(UUID announcementId);
}

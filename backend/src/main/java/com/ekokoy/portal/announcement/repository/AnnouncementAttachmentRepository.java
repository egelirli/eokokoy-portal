package com.ekokoy.portal.announcement.repository;

import com.ekokoy.portal.announcement.entity.AnnouncementAttachment;
import com.ekokoy.portal.announcement.entity.AttachmentFileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AnnouncementAttachmentRepository extends JpaRepository<AnnouncementAttachment, UUID> {

    List<AnnouncementAttachment> findByAnnouncementIdOrderByDisplayOrderAsc(UUID announcementId);

    long countByAnnouncementIdAndFileType(UUID announcementId, AttachmentFileType fileType);

    @Query("SELECT SUM(a.fileSize) FROM AnnouncementAttachment a WHERE a.announcement.id = :announcementId AND a.fileType = :fileType AND a.fileSize IS NOT NULL")
    Long sumFileSizeByAnnouncementIdAndFileType(UUID announcementId, AttachmentFileType fileType);
}

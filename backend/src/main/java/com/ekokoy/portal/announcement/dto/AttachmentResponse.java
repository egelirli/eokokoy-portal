package com.ekokoy.portal.announcement.dto;

import com.ekokoy.portal.announcement.entity.AnnouncementAttachment;
import com.ekokoy.portal.announcement.entity.AttachmentFileType;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        String fileUrl,
        String fileName,
        AttachmentFileType fileType,
        Integer fileSize,
        int displayOrder,
        Instant createdAt
) {
    public static AttachmentResponse from(AnnouncementAttachment a) {
        return new AttachmentResponse(
                a.getId(),
                a.getFileUrl(),
                a.getFileName(),
                a.getFileType(),
                a.getFileSize(),
                a.getDisplayOrder(),
                a.getCreatedAt()
        );
    }
}

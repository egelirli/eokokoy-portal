package com.ekokoy.portal.announcement.dto;

import com.ekokoy.portal.announcement.entity.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnnouncementResponse(
        UUID id,
        String title,
        String body,
        AnnouncementPriority priority,
        AnnouncementStatus status,
        boolean isPublic,
        AnnouncementTargetType targetType,
        Instant publishedAt,
        Instant scheduledAt,
        Instant expiresAt,
        Instant archivedAt,
        UUID createdById,
        String createdByName,
        Instant createdAt,
        Instant updatedAt,
        List<AttachmentResponse> attachments,
        List<AnnouncementTargetResponse> targets
) {
    public static AnnouncementResponse from(Announcement a) {
        String createdByName = a.getCreatedBy() != null
                ? a.getCreatedBy().getFirstName() + " " + a.getCreatedBy().getLastName()
                : null;
        return new AnnouncementResponse(
                a.getId(),
                a.getTitle(),
                a.getBody(),
                a.getPriority(),
                a.getStatus(),
                a.isPublic(),
                a.getTargetType(),
                a.getPublishedAt(),
                a.getScheduledAt(),
                a.getExpiresAt(),
                a.getArchivedAt(),
                a.getCreatedBy() != null ? a.getCreatedBy().getId() : null,
                createdByName,
                a.getCreatedAt(),
                a.getUpdatedAt(),
                a.getAttachments().stream().map(AttachmentResponse::from).toList(),
                a.getTargets().stream().map(AnnouncementTargetResponse::from).toList()
        );
    }
}

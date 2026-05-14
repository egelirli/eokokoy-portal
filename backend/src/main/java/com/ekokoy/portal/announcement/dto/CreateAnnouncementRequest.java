package com.ekokoy.portal.announcement.dto;

import com.ekokoy.portal.announcement.entity.AnnouncementPriority;
import com.ekokoy.portal.announcement.entity.AnnouncementTargetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateAnnouncementRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String body,
        @NotNull AnnouncementPriority priority,
        boolean isPublic,
        @NotNull AnnouncementTargetType targetType,
        Instant scheduledAt,
        Instant expiresAt,
        @Valid List<AnnouncementTargetRequest> targets
) {}

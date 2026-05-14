package com.ekokoy.portal.announcement.dto;

import com.ekokoy.portal.announcement.entity.AnnouncementTargetEntityType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AnnouncementTargetRequest(
        @NotNull AnnouncementTargetEntityType targetEntityType,
        @NotNull UUID targetId
) {}

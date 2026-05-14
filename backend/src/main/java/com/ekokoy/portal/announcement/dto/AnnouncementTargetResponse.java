package com.ekokoy.portal.announcement.dto;

import com.ekokoy.portal.announcement.entity.AnnouncementTarget;
import com.ekokoy.portal.announcement.entity.AnnouncementTargetEntityType;

import java.util.UUID;

public record AnnouncementTargetResponse(
        UUID id,
        AnnouncementTargetEntityType targetEntityType,
        UUID targetId
) {
    public static AnnouncementTargetResponse from(AnnouncementTarget t) {
        return new AnnouncementTargetResponse(t.getId(), t.getTargetEntityType(), t.getTargetId());
    }
}

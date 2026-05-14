package com.ekokoy.portal.announcement.dto;

import java.time.Instant;
import java.util.UUID;

public record ReadStatusResponse(
        UUID userId,
        String userName,
        String userEmail,
        Instant readAt
) {}

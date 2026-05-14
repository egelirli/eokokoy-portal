package com.ekokoy.portal.user.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String deviceInfo,
        String ipAddress,
        Instant createdAt,
        Instant expiresAt
) {}

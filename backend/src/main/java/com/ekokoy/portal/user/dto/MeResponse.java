package com.ekokoy.portal.user.dto;

import com.ekokoy.portal.user.entity.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String profilePhotoUrl,
        UserStatus status,
        List<String> roles,
        List<String> permissions,
        List<UUID> propertyIds,
        Instant createdAt
) {}

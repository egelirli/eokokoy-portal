package com.ekokoy.portal.user.dto;

import com.ekokoy.portal.user.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserRoleResponse(
        UUID roleId,
        String roleCode,
        String displayName,
        Instant assignedAt,
        UUID assignedById
) {
    public static UserRoleResponse from(UserRole ur) {
        return new UserRoleResponse(
                ur.getRole().getId(),
                ur.getRole().getCode(),
                ur.getRole().getDisplayName(),
                ur.getAssignedAt(),
                ur.getAssignedBy() != null ? ur.getAssignedBy().getId() : null
        );
    }
}

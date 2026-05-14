package com.ekokoy.portal.user.dto;

import com.ekokoy.portal.user.entity.Role;

import java.util.List;
import java.util.UUID;

public record RoleWithPermissionsResponse(
        UUID id,
        String code,
        String displayName,
        String description,
        boolean isActive,
        List<PermissionResponse> permissions
) {
    public static RoleWithPermissionsResponse from(Role r) {
        List<PermissionResponse> perms = r.getPermissions().stream()
                .map(PermissionResponse::from)
                .sorted((a, b) -> a.code().compareTo(b.code()))
                .toList();
        return new RoleWithPermissionsResponse(
                r.getId(), r.getCode(), r.getDisplayName(), r.getDescription(), r.isActive(), perms
        );
    }
}

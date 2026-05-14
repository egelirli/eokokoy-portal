package com.ekokoy.portal.user.dto;

import com.ekokoy.portal.user.entity.Role;

import java.util.List;
import java.util.UUID;

public record RoleResponse(UUID id, String code, String displayName, String description, boolean isActive) {

    public static RoleResponse from(Role r) {
        return new RoleResponse(r.getId(), r.getCode(), r.getDisplayName(), r.getDescription(), r.isActive());
    }

    public static RoleResponse withPermissions(Role r, List<PermissionResponse> permissions) {
        return new RoleResponse(r.getId(), r.getCode(), r.getDisplayName(), r.getDescription(), r.isActive());
    }
}

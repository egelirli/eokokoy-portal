package com.ekokoy.portal.user.dto;

import com.ekokoy.portal.user.entity.Permission;

import java.util.UUID;

public record PermissionResponse(UUID id, String code, String category, String description) {

    public static PermissionResponse from(Permission p) {
        return new PermissionResponse(p.getId(), p.getCode(), p.getCategory(), p.getDescription());
    }
}

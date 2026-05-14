package com.ekokoy.portal.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignRoleRequest(@NotNull(message = "roleId zorunludur") UUID roleId) {}

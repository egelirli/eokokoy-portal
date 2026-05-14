package com.ekokoy.portal.user.dto;

import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        UserStatus status,
        Instant createdAt,
        Instant approvedAt,
        UUID approvedById
) {
    public static ApplicationResponse from(User u) {
        return new ApplicationResponse(
                u.getId(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getPhone(),
                u.getStatus(),
                u.getCreatedAt(),
                u.getApprovedAt(),
                u.getApprovedBy() != null ? u.getApprovedBy().getId() : null
        );
    }
}

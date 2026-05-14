package com.ekokoy.portal.user.dto;

import com.ekokoy.portal.user.entity.Invitation;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        String email,
        String roleCode,
        String roleDisplayName,
        UUID propertyId,
        String propertyUnitNumber,
        Instant createdAt,
        Instant expiresAt,
        boolean isUsed,
        Instant usedAt
) {
    public static InvitationResponse from(Invitation inv) {
        return new InvitationResponse(
                inv.getId(),
                inv.getEmail(),
                inv.getRole().getCode(),
                inv.getRole().getDisplayName(),
                inv.getProperty() != null ? inv.getProperty().getId() : null,
                inv.getProperty() != null ? inv.getProperty().getUnitNumber() : null,
                inv.getCreatedAt(),
                inv.getExpiresAt(),
                inv.isUsed(),
                inv.getUsedAt()
        );
    }
}

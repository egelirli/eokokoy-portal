package com.ekokoy.portal.user.dto;

import com.ekokoy.portal.user.entity.Invitation;

import java.time.Instant;
import java.util.UUID;

public record VerifyInvitationResponse(
        String email,
        String roleCode,
        String roleDisplayName,
        UUID propertyId,
        Integer propertyNumber,
        Instant expiresAt
) {
    public static VerifyInvitationResponse from(Invitation inv) {
        return new VerifyInvitationResponse(
                inv.getEmail(),
                inv.getRole().getCode(),
                inv.getRole().getDisplayName(),
                inv.getProperty() != null ? inv.getProperty().getId() : null,
                inv.getProperty() != null ? inv.getProperty().getNumber() : null,
                inv.getExpiresAt()
        );
    }
}

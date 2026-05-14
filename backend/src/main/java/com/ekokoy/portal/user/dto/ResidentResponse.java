package com.ekokoy.portal.user.dto;

import com.ekokoy.portal.user.entity.PropertyUser;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ResidentResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String relationType,
        BigDecimal ownershipPercentage,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt
) {
    public static ResidentResponse from(PropertyUser pu) {
        return new ResidentResponse(
                pu.getId(),
                pu.getUser().getId(),
                pu.getUser().getFirstName(),
                pu.getUser().getLastName(),
                pu.getUser().getEmail(),
                pu.getRelationType().name(),
                pu.getOwnershipPercentage(),
                pu.getStartDate(),
                pu.getEndDate(),
                pu.getCreatedAt()
        );
    }
}

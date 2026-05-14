package com.ekokoy.portal.user.dto;

import com.ekokoy.portal.user.entity.PropertyUser;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserPropertyResponse(
        UUID relationId,
        UUID propertyId,
        int propertyNumber,
        String propertyStatus,
        String relationType,
        BigDecimal ownershipPercentage,
        LocalDate startDate,
        Instant createdAt
) {
    public static UserPropertyResponse from(PropertyUser pu) {
        return new UserPropertyResponse(
                pu.getId(),
                pu.getProperty().getId(),
                pu.getProperty().getNumber(),
                pu.getProperty().getStatus().name(),
                pu.getRelationType().name(),
                pu.getOwnershipPercentage(),
                pu.getStartDate(),
                pu.getCreatedAt()
        );
    }
}

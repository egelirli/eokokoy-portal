package com.ekokoy.portal.user.dto;

import com.ekokoy.portal.user.entity.Property;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        int number,
        String type,
        BigDecimal areaM2,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static PropertyResponse from(Property p) {
        return new PropertyResponse(
                p.getId(),
                p.getNumber(),
                p.getType(),
                p.getAreaM2(),
                p.getDescription(),
                p.getStatus().name(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}

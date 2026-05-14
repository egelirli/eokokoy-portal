package com.ekokoy.portal.user.dto;

import com.ekokoy.portal.user.entity.Property;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PropertyDetailResponse(
        UUID id,
        int number,
        String type,
        BigDecimal areaM2,
        String description,
        String status,
        List<ResidentResponse> activeResidents,
        Instant createdAt,
        Instant updatedAt
) {
    public static PropertyDetailResponse from(Property p, List<ResidentResponse> residents) {
        return new PropertyDetailResponse(
                p.getId(),
                p.getNumber(),
                p.getType(),
                p.getAreaM2(),
                p.getDescription(),
                p.getStatus().name(),
                residents,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}

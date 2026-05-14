package com.ekokoy.portal.user.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AddResidentRequest(
        @NotNull UUID userId,
        @NotNull String relationType,
        BigDecimal ownershipPercentage,
        @NotNull LocalDate startDate,
        String notes
) {}

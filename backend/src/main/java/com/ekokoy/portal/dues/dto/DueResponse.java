package com.ekokoy.portal.dues.dto;

import com.ekokoy.portal.dues.entity.Due;
import com.ekokoy.portal.dues.entity.DueStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DueResponse(
        UUID id,
        UUID propertyId,
        Integer propertyNumber,
        UUID userId,
        int periodYear,
        Integer periodMonth,
        BigDecimal amount,
        DueStatus status,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate dueDate,
        LocalDate paidAt,
        String description,
        UUID importBatchId,
        Instant createdAt,
        Instant updatedAt
) {
    public static DueResponse from(Due d) {
        return new DueResponse(
                d.getId(),
                d.getProperty().getId(),
                d.getProperty().getNumber(),
                d.getUser() != null ? d.getUser().getId() : null,
                d.getPeriodYear(),
                d.getPeriodMonth(),
                d.getAmount(),
                d.getStatus(),
                d.getPaidAmount(),
                d.getAmount().subtract(d.getPaidAmount()),
                d.getDueDate(),
                d.getPaidAt(),
                d.getDescription(),
                d.getImportBatch() != null ? d.getImportBatch().getId() : null,
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}

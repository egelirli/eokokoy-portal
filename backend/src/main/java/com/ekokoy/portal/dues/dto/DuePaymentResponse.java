package com.ekokoy.portal.dues.dto;

import com.ekokoy.portal.dues.entity.DuePayment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DuePaymentResponse(
        UUID id,
        UUID dueId,
        BigDecimal amount,
        LocalDate paymentDate,
        String paymentMethod,
        String referenceNo,
        String notes,
        UUID recordedById,
        String recordedByName,
        Instant createdAt
) {
    public static DuePaymentResponse from(DuePayment p) {
        return new DuePaymentResponse(
                p.getId(),
                p.getDue().getId(),
                p.getAmount(),
                p.getPaymentDate(),
                p.getPaymentMethod(),
                p.getReferenceNo(),
                p.getNotes(),
                p.getRecordedBy().getId(),
                p.getRecordedBy().getFirstName() + " " + p.getRecordedBy().getLastName(),
                p.getCreatedAt()
        );
    }
}

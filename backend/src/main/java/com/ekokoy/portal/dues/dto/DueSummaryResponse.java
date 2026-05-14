package com.ekokoy.portal.dues.dto;

import java.math.BigDecimal;

public record DueSummaryResponse(
        long totalDues,
        long unpaidDues,
        long partiallyPaidDues,
        long paidDues,
        long cancelledDues,
        BigDecimal totalAmount,
        BigDecimal totalPaidAmount,
        BigDecimal totalRemainingAmount
) {}

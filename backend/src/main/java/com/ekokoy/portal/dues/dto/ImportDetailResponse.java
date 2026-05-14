package com.ekokoy.portal.dues.dto;

import com.ekokoy.portal.dues.entity.DueImport;
import com.ekokoy.portal.dues.entity.ImportStatus;

import java.time.Instant;
import java.util.UUID;

public record ImportDetailResponse(
        UUID id,
        String fileName,
        UUID importedById,
        String importedByName,
        int totalRows,
        int successRows,
        int errorRows,
        String errorDetails,
        ImportStatus status,
        Instant createdAt,
        Instant completedAt
) {
    public static ImportDetailResponse from(DueImport i) {
        return new ImportDetailResponse(
                i.getId(),
                i.getFileName(),
                i.getImportedBy().getId(),
                i.getImportedBy().getFirstName() + " " + i.getImportedBy().getLastName(),
                i.getTotalRows(),
                i.getSuccessRows(),
                i.getErrorRows(),
                i.getErrorDetails(),
                i.getStatus(),
                i.getCreatedAt(),
                i.getCompletedAt()
        );
    }
}

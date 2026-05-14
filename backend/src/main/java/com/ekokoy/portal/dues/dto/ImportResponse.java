package com.ekokoy.portal.dues.dto;

import com.ekokoy.portal.dues.entity.DueImport;
import com.ekokoy.portal.dues.entity.ImportStatus;

import java.time.Instant;
import java.util.UUID;

public record ImportResponse(
        UUID id,
        String fileName,
        UUID importedById,
        String importedByName,
        int totalRows,
        int successRows,
        int errorRows,
        ImportStatus status,
        Instant createdAt,
        Instant completedAt
) {
    public static ImportResponse from(DueImport i) {
        return new ImportResponse(
                i.getId(),
                i.getFileName(),
                i.getImportedBy().getId(),
                i.getImportedBy().getFirstName() + " " + i.getImportedBy().getLastName(),
                i.getTotalRows(),
                i.getSuccessRows(),
                i.getErrorRows(),
                i.getStatus(),
                i.getCreatedAt(),
                i.getCompletedAt()
        );
    }
}

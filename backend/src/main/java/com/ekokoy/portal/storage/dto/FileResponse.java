package com.ekokoy.portal.storage.dto;

import com.ekokoy.portal.storage.entity.FileType;
import com.ekokoy.portal.storage.entity.StoredFile;

import java.time.Instant;
import java.util.UUID;

public record FileResponse(
        UUID id,
        String bucket,
        String objectKey,
        String originalName,
        String mimeType,
        Long fileSize,
        FileType fileType,
        boolean isConfirmed,
        String accessUrl,
        String thumbnailUrl,
        String compressedUrl,
        UUID uploadedBy,
        Instant uploadedAt,
        String checksum,
        Instant createdAt
) {
    public static FileResponse from(StoredFile f, String accessUrl, String thumbnailUrl, String compressedUrl) {
        return new FileResponse(
                f.getId(),
                f.getBucket(),
                f.getObjectKey(),
                f.getOriginalName(),
                f.getMimeType(),
                f.getFileSize(),
                f.getFileType(),
                f.isConfirmed(),
                accessUrl,
                thumbnailUrl,
                compressedUrl,
                f.getUploadedBy().getId(),
                f.getUploadedAt(),
                f.getChecksum(),
                f.getCreatedAt()
        );
    }
}

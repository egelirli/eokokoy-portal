package com.ekokoy.portal.storage.dto;

import java.time.Instant;
import java.util.UUID;

public record UploadUrlResponse(
        UUID fileId,
        String presignedUrl,
        String objectKey,
        Instant expiresAt
) {}

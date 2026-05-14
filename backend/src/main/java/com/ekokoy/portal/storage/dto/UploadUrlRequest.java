package com.ekokoy.portal.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record UploadUrlRequest(
        @NotBlank String bucket,
        @NotBlank String module,
        @NotBlank String originalName,
        @NotBlank String mimeType,
        @Positive Long fileSize
) {}

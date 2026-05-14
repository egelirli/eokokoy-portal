package com.ekokoy.portal.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmUploadRequest(
        @NotBlank
        @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "checksum geçersiz SHA-256 formatı")
        String checksum
) {}

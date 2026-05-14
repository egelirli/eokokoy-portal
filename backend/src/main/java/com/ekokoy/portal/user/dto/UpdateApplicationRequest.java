package com.ekokoy.portal.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateApplicationRequest(
        @NotBlank(message = "Aksiyon boş olamaz")
        @Pattern(regexp = "approve|reject|request_info", message = "Aksiyon approve, reject veya request_info olmalıdır")
        String action,

        String message
) {}

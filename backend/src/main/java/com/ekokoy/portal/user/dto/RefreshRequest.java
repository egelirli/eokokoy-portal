package com.ekokoy.portal.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Refresh token zorunludur")
        String refreshToken
) {}

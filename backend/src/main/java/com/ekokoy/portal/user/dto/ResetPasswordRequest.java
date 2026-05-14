package com.ekokoy.portal.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Token zorunludur")
        String token,

        @NotBlank(message = "Yeni şifre zorunludur")
        @Size(min = 8, max = 72, message = "Şifre 8-72 karakter arasında olmalıdır")
        String newPassword
) {}

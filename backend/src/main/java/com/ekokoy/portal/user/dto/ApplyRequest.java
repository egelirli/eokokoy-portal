package com.ekokoy.portal.user.dto;

import jakarta.validation.constraints.*;

public record ApplyRequest(
        @NotBlank(message = "Ad boş olamaz")
        @Size(max = 100, message = "Ad en fazla 100 karakter olabilir")
        String firstName,

        @NotBlank(message = "Soyad boş olamaz")
        @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir")
        String lastName,

        @NotBlank(message = "E-posta boş olamaz")
        @Email(message = "Geçerli bir e-posta adresi giriniz")
        @Size(max = 255)
        String email,

        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Telefon E.164 formatında olmalıdır (örn: +905551234567)")
        String phone,

        @NotBlank(message = "Şifre boş olamaz")
        @Size(min = 8, message = "Şifre en az 8 karakter olmalıdır")
        String password
) {}

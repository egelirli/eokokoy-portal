package com.ekokoy.portal.user.controller;

import com.ekokoy.portal.common.ApiResponse;
import com.ekokoy.portal.user.dto.*;
import com.ekokoy.portal.user.service.ApplicationService;
import com.ekokoy.portal.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final ApplicationService applicationService;
    private final AuthService authService;

    public AuthController(ApplicationService applicationService, AuthService authService) {
        this.applicationService = applicationService;
        this.authService = authService;
    }

    /** Yeni üyelik başvurusu — herkese açık. */
    @PostMapping("/apply")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(
            @Valid @RequestBody ApplyRequest request) {
        ApplicationResponse response = applicationService.apply(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response,
                "Başvurunuz alındı. Yönetim kurulu incelemesinin ardından e-posta ile bilgilendirileceksiniz."));
    }

    /** Giriş yap — access + refresh token döner. */
    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.ok(response, null));
    }

    /** Refresh token ile yeni token çifti üretir (rotation). */
    @PostMapping("/refresh")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.refresh(request, getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.ok(response, null));
    }

    /** Mevcut oturumu kapatır. */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Çıkış yapıldı."));
    }

    /** Şifre sıfırlama e-postası gönderir — herkese açık. */
    @PostMapping("/forgot-password")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        authService.forgotPassword(request, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok(null,
                "Şifre sıfırlama bağlantısı e-posta adresinize gönderildi."));
    }

    /** Token ile şifre sıfırlar — herkese açık. */
    @PostMapping("/reset-password")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        authService.resetPassword(request, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok(null, "Şifreniz başarıyla güncellendi."));
    }

    /** Mevcut kullanıcı bilgilerini döner. */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MeResponse>> getMe() {
        return ResponseEntity.ok(ApiResponse.ok(authService.getMe(), null));
    }

    /** Mevcut kullanıcının aktif oturumlarını listeler. */
    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions() {
        return ResponseEntity.ok(ApiResponse.ok(authService.getSessions(), null));
    }

    /** Belirli bir oturumu kapatır. */
    @DeleteMapping("/sessions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable UUID id) {
        authService.revokeSession(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Oturum kapatıldı."));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

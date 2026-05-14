package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.config.JwtProperties;
import com.ekokoy.portal.config.JwtUtil;
import com.ekokoy.portal.user.dto.*;
import com.ekokoy.portal.user.entity.*;
import com.ekokoy.portal.user.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final String DUMMY_HASH = "$2b$12$invalidhashfortimingattackpreventi0nXXXXXXXXXXXXXXXXXX";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PropertyUserRepository propertyUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final LoginRateLimiterService loginRateLimiterService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PropertyUserRepository propertyUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       JwtProperties jwtProperties,
                       LoginRateLimiterService loginRateLimiterService,
                       EmailService emailService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.propertyUserRepository = propertyUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.loginRateLimiterService = loginRateLimiterService;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    /**
     * Kullanıcı girişi. Access token + refresh token döner.
     * Timing attack önlemi: kullanıcı bulunamasa da passwordEncoder.matches() çalışır.
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String deviceInfo) {
        loginRateLimiterService.checkAndConsume(request.email());

        User user = userRepository.findByIdWithRolesAndPermissions(
                userRepository.findByEmailAndIsDeletedFalse(request.email())
                        .map(User::getId)
                        .orElse(UUID.randomUUID())
        ).orElse(null);

        String hashToCheck = (user != null && user.getPasswordHash() != null)
                ? user.getPasswordHash()
                : DUMMY_HASH;

        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null || !passwordMatches) {
            throw new EkokoyException("INVALID_CREDENTIALS", "E-posta veya şifre hatalı.", 401);
        }

        if (user.getStatus() != UserStatus.active) {
            throw new EkokoyException("ACCOUNT_NOT_ACTIVE", "Hesabınız aktif değil.", 403);
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String rawRefreshToken = generateRawToken();
        RefreshToken refreshToken = createRefreshToken(user, rawRefreshToken, ipAddress, deviceInfo);
        refreshTokenRepository.save(refreshToken);

        String accessToken = buildAccessToken(user);

        auditLogService.log(user.getId(), "LOGIN", "users", user.getId(),
                "{\"ip\":\"" + ipAddress + "\"}");

        return new LoginResponse(accessToken, rawRefreshToken, "Bearer", jwtProperties.getAccessExpiration());
    }

    /**
     * Refresh token ile yeni access token + refresh token üretir (rotation).
     * Revoke edilmiş token tespitinde tüm oturumlar kapatılır.
     */
    @Transactional
    public LoginResponse refresh(RefreshRequest request, String ipAddress, String deviceInfo) {
        String tokenHash = sha256(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new EkokoyException("INVALID_REFRESH_TOKEN", "Geçersiz refresh token.", 401));

        if (stored.getRevokedAt() != null) {
            // Token hırsızlığı tespiti → tüm oturumları kapat
            refreshTokenRepository.revokeAllByUserId(stored.getUser().getId(), Instant.now());
            auditLogService.log(stored.getUser().getId(), "TOKEN_THEFT_DETECTED", "refresh_tokens",
                    stored.getId(), "{\"ip\":\"" + ipAddress + "\"}");
            throw new EkokoyException("TOKEN_REUSE_DETECTED",
                    "Güvenlik ihlali tespit edildi. Tüm oturumlarınız kapatıldı.", 401);
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new EkokoyException("REFRESH_TOKEN_EXPIRED", "Refresh token süresi dolmuş.", 401);
        }

        User user = userRepository.findByIdWithRolesAndPermissions(stored.getUser().getId())
                .orElseThrow(() -> new EkokoyException("USER_NOT_FOUND", "Kullanıcı bulunamadı.", 404));

        if (user.getStatus() != UserStatus.active) {
            throw new EkokoyException("ACCOUNT_NOT_ACTIVE", "Hesabınız aktif değil.", 403);
        }

        // Eski token'ı revoke et
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        // Yeni token'lar üret
        String rawRefreshToken = generateRawToken();
        RefreshToken newRefreshToken = createRefreshToken(user, rawRefreshToken, ipAddress, deviceInfo);
        refreshTokenRepository.save(newRefreshToken);

        String accessToken = buildAccessToken(user);

        return new LoginResponse(accessToken, rawRefreshToken, "Bearer", jwtProperties.getAccessExpiration());
    }

    /**
     * Mevcut oturumu kapatır (refresh token revoke edilir).
     */
    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
            auditLogService.log(token.getUser().getId(), "LOGOUT", "refresh_tokens", token.getId(), null);
        });
    }

    /**
     * Şifre sıfırlama e-postası gönderir.
     * Kullanıcı bulunamasa da aynı yanıt döner (user enumeration önlemi).
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, String ipAddress) {
        userRepository.findByEmailAndIsDeletedFalse(request.email()).ifPresent(user -> {
            if (user.getStatus() != UserStatus.active) return;

            String rawToken = generateRawToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(sha256(rawToken));
            resetToken.setExpiresAt(Instant.now().plusSeconds(3600)); // 1 saat
            resetToken.setIpAddress(ipAddress);
            passwordResetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), rawToken);
            auditLogService.log(user.getId(), "PASSWORD_RESET_REQUEST", "users", user.getId(),
                    "{\"ip\":\"" + ipAddress + "\"}");
        });
    }

    /**
     * Şifre sıfırlama token'ını doğrulayarak yeni şifreyi uygular.
     * Tüm refresh token'lar revoke edilir.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request, String ipAddress) {
        validatePasswordPolicy(request.newPassword());

        String tokenHash = sha256(request.token());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new EkokoyException("INVALID_RESET_TOKEN", "Geçersiz veya süresi dolmuş token.", 400));

        if (resetToken.isUsed()) {
            throw new EkokoyException("RESET_TOKEN_USED", "Bu token daha önce kullanılmış.", 400);
        }
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new EkokoyException("RESET_TOKEN_EXPIRED", "Token süresi dolmuş.", 400);
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Şifre değişince tüm oturumları kapat
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());

        auditLogService.log(user.getId(), "PASSWORD_RESET", "users", user.getId(),
                "{\"ip\":\"" + ipAddress + "\"}");
    }

    /**
     * Mevcut oturumdaki kullanıcının bilgilerini döner.
     */
    @Transactional(readOnly = true)
    public MeResponse getMe() {
        UUID userId = currentUserId();
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new EkokoyException("USER_NOT_FOUND", "Kullanıcı bulunamadı.", 404));

        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getCode())
                .distinct().toList();

        List<String> permissions = user.getUserRoles().stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(p -> p.getCode())
                .distinct().toList();

        List<UUID> propertyIds = propertyUserRepository.findActiveByUserId(userId).stream()
                .map(pu -> pu.getProperty().getId())
                .toList();

        return new MeResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfilePhotoUrl(),
                user.getStatus(),
                roles,
                permissions,
                propertyIds,
                user.getCreatedAt()
        );
    }

    /**
     * Mevcut kullanıcının aktif oturumlarını listeler.
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> getSessions() {
        UUID userId = currentUserId();
        return refreshTokenRepository.findActiveByUserId(userId, Instant.now()).stream()
                .map(rt -> new SessionResponse(
                        rt.getId(),
                        rt.getDeviceInfo(),
                        rt.getIpAddress(),
                        rt.getCreatedAt(),
                        rt.getExpiresAt()
                ))
                .toList();
    }

    /**
     * Belirli bir oturumu kapatır. Sadece kendi oturumu kapatılabilir.
     */
    @Transactional
    public void revokeSession(UUID sessionId) {
        UUID userId = currentUserId();
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new EkokoyException("SESSION_NOT_FOUND", "Oturum bulunamadı.", 404));

        if (!token.getUser().getId().equals(userId)) {
            throw new EkokoyException("ACCESS_DENIED", "Bu oturumu kapatma yetkiniz yok.", 403);
        }

        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        auditLogService.log(userId, "SESSION_REVOKED", "refresh_tokens", sessionId, null);
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    private String buildAccessToken(User user) {
        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getCode())
                .distinct().toList();

        List<String> permissions = user.getUserRoles().stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(p -> p.getCode())
                .distinct().toList();

        List<UUID> propertyIds = propertyUserRepository.findActiveByUserId(user.getId()).stream()
                .map(pu -> pu.getProperty().getId())
                .toList();

        return jwtUtil.generateAccessToken(user.getId(), user.getEmail(), roles, permissions, propertyIds);
    }

    private RefreshToken createRefreshToken(User user, String rawToken, String ipAddress, String deviceInfo) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(sha256(rawToken));
        rt.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshExpiration()));
        rt.setIpAddress(ipAddress);
        rt.setDeviceInfo(deviceInfo);
        return rt;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 desteklenmiyor", e);
        }
    }

    private UUID currentUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private void validatePasswordPolicy(String password) {
        if (password.length() < 8 || password.length() > 72) {
            throw new EkokoyException("WEAK_PASSWORD", "Şifre 8-72 karakter arasında olmalıdır.", 422);
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new EkokoyException("WEAK_PASSWORD", "Şifre en az bir büyük harf içermelidir.", 422);
        }
        if (!password.matches(".*[a-z].*")) {
            throw new EkokoyException("WEAK_PASSWORD", "Şifre en az bir küçük harf içermelidir.", 422);
        }
        if (!password.matches(".*[0-9].*")) {
            throw new EkokoyException("WEAK_PASSWORD", "Şifre en az bir rakam içermelidir.", 422);
        }
    }
}

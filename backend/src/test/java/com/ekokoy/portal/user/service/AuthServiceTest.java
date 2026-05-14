package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.config.JwtProperties;
import com.ekokoy.portal.config.JwtUtil;
import com.ekokoy.portal.user.dto.*;
import com.ekokoy.portal.user.entity.*;
import com.ekokoy.portal.user.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PropertyUserRepository propertyUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private JwtProperties jwtProperties;
    @Mock private LoginRateLimiterService loginRateLimiterService;
    @Mock private EmailService emailService;
    @Mock private AuditLogService auditLogService;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private AuthService authService;

    private final UUID userId = UUID.randomUUID();
    private final String userEmail = "user@ekokoy.com";
    private final String rawPassword = "Sifre123!";
    private final String passwordHash = "$2b$12$hashedpassword";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn(userId.toString());
        lenient().when(jwtProperties.getAccessExpiration()).thenReturn(900L);
        lenient().when(jwtProperties.getRefreshExpiration()).thenReturn(2592000L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Login ────────────────────────────────────────────────────────────────────

    @Test
    void should_return_tokens_when_login_with_valid_credentials() {
        User user = makeActiveUser();
        when(userRepository.findByEmailAndIsDeletedFalse(userEmail)).thenReturn(Optional.of(user));
        when(userRepository.findByIdWithRolesAndPermissions(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, passwordHash)).thenReturn(true);
        when(userRepository.save(any())).thenReturn(user);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(propertyUserRepository.findActiveByUserId(userId)).thenReturn(List.of());
        when(jwtUtil.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("access-token");

        LoginResponse result = authService.login(new LoginRequest(userEmail, rawPassword), "127.0.0.1", "TestAgent");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.tokenType()).isEqualTo("Bearer");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(auditLogService).log(eq(userId), eq("LOGIN"), eq("users"), eq(userId), anyString());
    }

    @Test
    void should_throw_when_login_with_wrong_password() {
        User user = makeActiveUser();
        when(userRepository.findByEmailAndIsDeletedFalse(userEmail)).thenReturn(Optional.of(user));
        when(userRepository.findByIdWithRolesAndPermissions(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, passwordHash)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(userEmail, rawPassword), "127.0.0.1", null))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("E-posta veya şifre hatalı");
    }

    @Test
    void should_throw_same_error_when_login_with_nonexistent_email() {
        when(userRepository.findByEmailAndIsDeletedFalse("nobody@ekokoy.com")).thenReturn(Optional.empty());
        when(userRepository.findByIdWithRolesAndPermissions(any())).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@ekokoy.com", rawPassword), "127.0.0.1", null))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("E-posta veya şifre hatalı");

        // Timing attack önlemi: passwordEncoder mutlaka çağrılmalı
        verify(passwordEncoder).matches(anyString(), anyString());
    }

    @Test
    void should_throw_when_login_with_inactive_user() {
        User user = makeUserWithStatus(UserStatus.pending);
        when(userRepository.findByEmailAndIsDeletedFalse(userEmail)).thenReturn(Optional.of(user));
        when(userRepository.findByIdWithRolesAndPermissions(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, passwordHash)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest(userEmail, rawPassword), "127.0.0.1", null))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("aktif değil");
    }

    @Test
    void should_check_rate_limit_on_login() {
        doThrow(new EkokoyException("LOGIN_RATE_LIMIT", "Çok fazla deneme.", 429))
                .when(loginRateLimiterService).checkAndConsume(userEmail);

        assertThatThrownBy(() -> authService.login(new LoginRequest(userEmail, rawPassword), "127.0.0.1", null))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("LOGIN_RATE_LIMIT");
    }

    // ── Refresh ──────────────────────────────────────────────────────────────────

    @Test
    void should_rotate_refresh_token_on_refresh() {
        User user = makeActiveUser();
        RefreshToken stored = makeActiveRefreshToken(user);
        String rawToken = "raw-refresh-token";

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(userRepository.findByIdWithRolesAndPermissions(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(propertyUserRepository.findActiveByUserId(userId)).thenReturn(List.of());
        when(jwtUtil.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("new-access-token");

        LoginResponse result = authService.refresh(new RefreshRequest(rawToken), "127.0.0.1", null);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isNotBlank();
        // Eski token revoke edilmeli
        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void should_revoke_all_sessions_when_revoked_token_reused() {
        User user = makeActiveUser();
        RefreshToken revokedToken = makeActiveRefreshToken(user);
        revokedToken.setRevokedAt(Instant.now().minusSeconds(60)); // önceden revoke edilmiş

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("stolen-token"), "127.0.0.1", null))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("TOKEN_REUSE_DETECTED");

        verify(refreshTokenRepository).revokeAllByUserId(eq(userId), any(Instant.class));
        verify(auditLogService).log(eq(userId), eq("TOKEN_THEFT_DETECTED"), eq("refresh_tokens"), any(), anyString());
    }

    @Test
    void should_throw_when_refresh_token_expired() {
        User user = makeActiveUser();
        RefreshToken expired = makeActiveRefreshToken(user);
        expired.setExpiresAt(Instant.now().minusSeconds(3600)); // süresi dolmuş

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("expired-token"), "127.0.0.1", null))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("REFRESH_TOKEN_EXPIRED");
    }

    // ── Logout ───────────────────────────────────────────────────────────────────

    @Test
    void should_revoke_token_on_logout() {
        User user = makeActiveUser();
        RefreshToken token = makeActiveRefreshToken(user);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.logout(new LogoutRequest("valid-token"));

        assertThat(token.getRevokedAt()).isNotNull();
        verify(auditLogService).log(eq(userId), eq("LOGOUT"), eq("refresh_tokens"), any(), isNull());
    }

    @Test
    void should_silently_succeed_when_logout_with_invalid_token() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // İstisna fırlatmamalı
        assertThatCode(() -> authService.logout(new LogoutRequest("invalid-token"))).doesNotThrowAnyException();
    }

    // ── Forgot Password ──────────────────────────────────────────────────────────

    @Test
    void should_send_reset_email_when_user_exists() {
        User user = makeActiveUser();
        when(userRepository.findByEmailAndIsDeletedFalse(userEmail)).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.forgotPassword(new ForgotPasswordRequest(userEmail), "127.0.0.1");

        verify(emailService).sendPasswordResetEmail(eq(userEmail), eq("Test"), anyString());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void should_not_throw_when_forgot_password_with_unknown_email() {
        when(userRepository.findByEmailAndIsDeletedFalse("nobody@ekokoy.com")).thenReturn(Optional.empty());

        // User enumeration önlemi: istisna fırlatmamalı
        assertThatCode(() -> authService.forgotPassword(new ForgotPasswordRequest("nobody@ekokoy.com"), "127.0.0.1"))
                .doesNotThrowAnyException();

        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    // ── Reset Password ───────────────────────────────────────────────────────────

    @Test
    void should_update_password_and_revoke_sessions_on_reset() {
        User user = makeActiveUser();
        PasswordResetToken resetToken = makeValidPasswordResetToken(user);
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NewPass1!")).thenReturn("$newhash");
        when(userRepository.save(any())).thenReturn(user);
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.resetPassword(new ResetPasswordRequest("raw-token", "NewPass1!"), "127.0.0.1");

        assertThat(user.getPasswordHash()).isEqualTo("$newhash");
        assertThat(resetToken.isUsed()).isTrue();
        verify(refreshTokenRepository).revokeAllByUserId(eq(userId), any(Instant.class));
        verify(auditLogService).log(eq(userId), eq("PASSWORD_RESET"), eq("users"), eq(userId), anyString());
    }

    @Test
    void should_throw_when_reset_token_already_used() {
        User user = makeActiveUser();
        PasswordResetToken usedToken = makeValidPasswordResetToken(user);
        usedToken.setUsed(true);
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("used-token", "NewPass1!"), "127.0.0.1"))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("RESET_TOKEN_USED");
    }

    @Test
    void should_throw_when_reset_token_expired() {
        User user = makeActiveUser();
        PasswordResetToken expiredToken = makeValidPasswordResetToken(user);
        expiredToken.setExpiresAt(Instant.now().minusSeconds(3600));
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("expired-token", "NewPass1!"), "127.0.0.1"))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("RESET_TOKEN_EXPIRED");
    }

    @Test
    void should_throw_when_reset_password_violates_policy() {
        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("token", "weak"), "127.0.0.1"))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("WEAK_PASSWORD");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private User makeActiveUser() {
        return makeUserWithStatus(UserStatus.active);
    }

    private User makeUserWithStatus(UserStatus status) {
        User u = new User();
        u.setId(userId);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail(userEmail);
        u.setPasswordHash(passwordHash);
        u.setStatus(status);
        u.setUserRoles(new ArrayList<>());
        return u;
    }

    private RefreshToken makeActiveRefreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash("token-hash");
        rt.setExpiresAt(Instant.now().plusSeconds(2592000));
        return rt;
    }

    private PasswordResetToken makeValidPasswordResetToken(User user) {
        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setTokenHash("reset-token-hash");
        prt.setExpiresAt(Instant.now().plusSeconds(3600));
        prt.setUsed(false);
        return prt;
    }
}

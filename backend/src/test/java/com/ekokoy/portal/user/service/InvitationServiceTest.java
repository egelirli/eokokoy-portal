package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.user.dto.CompleteInvitationRequest;
import com.ekokoy.portal.user.dto.CreateInvitationRequest;
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
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock private InvitationRepository invitationRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private AuditLogService auditLogService;
    @Mock private TokenRateLimiterService rateLimiterService;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private InvitationService invitationService;

    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(actorId.toString());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Role makeRole(String code) {
        Role r = new Role();
        r.setId(UUID.randomUUID());
        r.setCode(code);
        r.setDisplayName(code);
        r.setActive(true);
        return r;
    }

    private User makeUser(UUID id, String email) {
        User u = new User();
        u.setId(id);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail(email);
        u.setStatus(UserStatus.active);
        return u;
    }

    private Invitation makeInvitation(String email, Role role, boolean used, Instant expiresAt) {
        Invitation inv = new Invitation();
        inv.setId(UUID.randomUUID());
        inv.setEmail(email);
        inv.setToken("somehash");
        inv.setRole(role);
        inv.setCreatedBy(makeUser(UUID.randomUUID(), "admin@ekokoy.com"));
        inv.setExpiresAt(expiresAt);
        inv.setUsed(used);
        return inv;
    }

    @Test
    void should_create_invitation_and_send_email_when_valid_request() {
        Role role = makeRole("EV_SAHIBI");
        User actor = makeUser(actorId, "admin@ekokoy.com");

        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> {
            Invitation i = inv.getArgument(0);
            i.setId(UUID.randomUUID());
            return i;
        });

        var result = invitationService.createInvitation(
                new CreateInvitationRequest("user@ekokoy.com", role.getId(), null));

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("user@ekokoy.com");
        assertThat(result.roleCode()).isEqualTo("EV_SAHIBI");
        verify(emailService).sendInvitationEmail(eq("user@ekokoy.com"), anyString(), eq("EV_SAHIBI"));
    }

    @Test
    void should_throw_when_creating_super_admin_invitation() {
        Role superAdmin = makeRole("SUPER_ADMIN");

        when(roleRepository.findById(superAdmin.getId())).thenReturn(Optional.of(superAdmin));

        assertThatThrownBy(() -> invitationService.createInvitation(
                new CreateInvitationRequest("user@ekokoy.com", superAdmin.getId(), null)))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("SUPER_ADMIN rolü davet ile atanamaz");
    }

    @Test
    void should_throw_when_verifying_expired_token() {
        Role role = makeRole("EV_SAHIBI");
        Invitation expired = makeInvitation("user@ekokoy.com", role, false,
                Instant.now().minus(1, ChronoUnit.HOURS));

        // rateLimiter izin verir
        doNothing().when(rateLimiterService).assertNotBlocked(anyString());
        when(invitationRepository.findByTokenWithRole(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> invitationService.verifyToken("somerawtoken"))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("süresi dolmuştur");
    }

    @Test
    void should_throw_when_verifying_used_token() {
        Role role = makeRole("EV_SAHIBI");
        Invitation used = makeInvitation("user@ekokoy.com", role, true,
                Instant.now().plus(24, ChronoUnit.HOURS));

        doNothing().when(rateLimiterService).assertNotBlocked(anyString());
        when(invitationRepository.findByTokenWithRole(anyString())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> invitationService.verifyToken("somerawtoken"))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("daha önce kullanılmıştır");
    }

    @Test
    void should_complete_registration_when_valid_token() {
        Role role = makeRole("EV_SAHIBI");
        Invitation inv = makeInvitation("user@ekokoy.com", role, false,
                Instant.now().plus(24, ChronoUnit.HOURS));

        doNothing().when(rateLimiterService).assertNotBlocked(anyString());
        when(invitationRepository.findByTokenWithRole(anyString())).thenReturn(Optional.of(inv));
        when(userRepository.existsByEmailAndIsDeletedFalse("user@ekokoy.com")).thenReturn(false);
        when(passwordEncoder.encode("Sifre123!")).thenReturn("$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(i -> i.getArgument(0));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        var result = invitationService.completeInvitation(
                new CompleteInvitationRequest("somerawtoken", "Ali", "Veli", null, "Sifre123!"));

        assertThat(result.email()).isEqualTo("user@ekokoy.com");
        assertThat(result.status()).isEqualTo(UserStatus.active);
        assertThat(inv.isUsed()).isTrue();
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void should_throw_when_completing_with_already_registered_email() {
        Role role = makeRole("EV_SAHIBI");
        Invitation inv = makeInvitation("user@ekokoy.com", role, false,
                Instant.now().plus(24, ChronoUnit.HOURS));

        doNothing().when(rateLimiterService).assertNotBlocked(anyString());
        when(invitationRepository.findByTokenWithRole(anyString())).thenReturn(Optional.of(inv));
        when(userRepository.existsByEmailAndIsDeletedFalse("user@ekokoy.com")).thenReturn(true);

        assertThatThrownBy(() -> invitationService.completeInvitation(
                new CompleteInvitationRequest("somerawtoken", "Ali", "Veli", null, "Sifre123!")))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("e-posta adresi zaten kayıtlı");
    }

    @Test
    void should_throw_when_rate_limit_exceeded() {
        doThrow(new EkokoyException("TOKEN_RATE_LIMIT", "Çok fazla başarısız deneme. 15 dakika sonra tekrar deneyiniz.", 429))
                .when(rateLimiterService).assertNotBlocked(anyString());

        assertThatThrownBy(() -> invitationService.verifyToken("anytoken"))
                .isInstanceOf(EkokoyException.class)
                .extracting(e -> ((EkokoyException) e).getCode())
                .isEqualTo("TOKEN_RATE_LIMIT");
    }

    @Test
    void should_record_failed_attempt_when_token_not_found() {
        doNothing().when(rateLimiterService).assertNotBlocked(anyString());
        when(invitationRepository.findByTokenWithRole(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.verifyToken("wrongtoken"))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("Geçersiz");

        verify(rateLimiterService).recordFailedAttempt(anyString());
    }
}

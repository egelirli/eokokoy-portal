package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.user.dto.ApplyRequest;
import com.ekokoy.portal.user.dto.UpdateApplicationRequest;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserStatus;
import com.ekokoy.portal.user.repository.UserRepository;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private AuditLogService auditLogService;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private ApplicationService applicationService;

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

    private User makeUser(UUID id, String email, UserStatus status) {
        User u = new User();
        u.setId(id);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail(email);
        u.setStatus(status);
        return u;
    }

    @Test
    void should_create_pending_user_when_apply_with_valid_request() {
        ApplyRequest request = new ApplyRequest("Ali", "Veli", "ali@ekokoy.com", null, "Sifre123!");

        when(userRepository.existsByEmailAndIsDeletedFalse("ali@ekokoy.com")).thenReturn(false);
        when(passwordEncoder.encode("Sifre123!")).thenReturn("$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        var result = applicationService.apply(request);

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("ali@ekokoy.com");
        assertThat(result.status()).isEqualTo(UserStatus.pending);
        verify(userRepository).save(any(User.class));
        verify(auditLogService).log(isNull(), eq("USER_APPLY"), eq("users"), any(), anyString());
    }

    @Test
    void should_throw_when_apply_with_duplicate_email() {
        ApplyRequest request = new ApplyRequest("Ali", "Veli", "ali@ekokoy.com", null, "Sifre123!");
        when(userRepository.existsByEmailAndIsDeletedFalse("ali@ekokoy.com")).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(request))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("e-posta adresi zaten kayıtlı");
    }

    @Test
    void should_activate_user_when_approve_pending_application() {
        UUID applicantId = UUID.randomUUID();
        User applicant = makeUser(applicantId, "applicant@ekokoy.com", UserStatus.pending);
        User actor = makeUser(actorId, "admin@ekokoy.com", UserStatus.active);

        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = applicationService.updateApplication(applicantId,
                new UpdateApplicationRequest("approve", null));

        assertThat(result.status()).isEqualTo(UserStatus.active);
        verify(emailService).sendApprovalEmail(eq("applicant@ekokoy.com"), eq("Test"));
        verify(auditLogService).log(eq(actorId), eq("APPLICATION_APPROVE"), eq("users"), eq(applicantId), anyString());
    }

    @Test
    void should_set_inactive_when_reject_application() {
        UUID applicantId = UUID.randomUUID();
        User applicant = makeUser(applicantId, "applicant@ekokoy.com", UserStatus.pending);
        User actor = makeUser(actorId, "admin@ekokoy.com", UserStatus.active);

        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = applicationService.updateApplication(applicantId,
                new UpdateApplicationRequest("reject", "Eksik belge"));

        assertThat(result.status()).isEqualTo(UserStatus.inactive);
        verify(emailService).sendRejectionEmail(eq("applicant@ekokoy.com"), eq("Test"), eq("Eksik belge"));
    }

    @Test
    void should_send_info_email_when_request_info() {
        UUID applicantId = UUID.randomUUID();
        User applicant = makeUser(applicantId, "applicant@ekokoy.com", UserStatus.pending);
        User actor = makeUser(actorId, "admin@ekokoy.com", UserStatus.active);

        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.updateApplication(applicantId,
                new UpdateApplicationRequest("request_info", "Kimlik belgesi gönderin"));

        verify(emailService).sendRequestInfoEmail(eq("applicant@ekokoy.com"), eq("Test"), eq("Kimlik belgesi gönderin"));
        verify(auditLogService).log(eq(actorId), eq("APPLICATION_REQUEST_INFO"), eq("users"), eq(applicantId), anyString());
    }

    @Test
    void should_throw_when_request_info_without_message() {
        UUID applicantId = UUID.randomUUID();
        User applicant = makeUser(applicantId, "applicant@ekokoy.com", UserStatus.pending);
        User actor = makeUser(actorId, "admin@ekokoy.com", UserStatus.active);

        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        assertThatThrownBy(() -> applicationService.updateApplication(applicantId,
                new UpdateApplicationRequest("request_info", null)))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("mesaj zorunludur");
    }

    @Test
    void should_throw_when_approving_already_active_user() {
        UUID applicantId = UUID.randomUUID();
        User applicant = makeUser(applicantId, "applicant@ekokoy.com", UserStatus.active);
        User actor = makeUser(actorId, "admin@ekokoy.com", UserStatus.active);

        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        assertThatThrownBy(() -> applicationService.updateApplication(applicantId,
                new UpdateApplicationRequest("approve", null)))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("Sadece beklemedeki başvurular");
    }
}

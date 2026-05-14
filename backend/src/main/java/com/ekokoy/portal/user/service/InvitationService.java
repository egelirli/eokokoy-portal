package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.user.dto.ApplicationResponse;
import com.ekokoy.portal.user.dto.CompleteInvitationRequest;
import com.ekokoy.portal.user.dto.CreateInvitationRequest;
import com.ekokoy.portal.user.dto.InvitationResponse;
import com.ekokoy.portal.user.dto.VerifyInvitationResponse;
import com.ekokoy.portal.user.entity.*;
import com.ekokoy.portal.user.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InvitationService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final int INVITATION_TTL_HOURS = 48;

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PropertyRepository propertyRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final TokenRateLimiterService rateLimiterService;

    public InvitationService(InvitationRepository invitationRepository,
                              UserRepository userRepository,
                              RoleRepository roleRepository,
                              PropertyRepository propertyRepository,
                              UserRoleRepository userRoleRepository,
                              PasswordEncoder passwordEncoder,
                              EmailService emailService,
                              AuditLogService auditLogService,
                              TokenRateLimiterService rateLimiterService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.propertyRepository = propertyRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Yeni davet oluşturur ve e-posta gönderir.
     * SUPER_ADMIN rolü davet ile atanamaz.
     */
    @Transactional
    public InvitationResponse createInvitation(CreateInvitationRequest request) {
        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new EkokoyException("ROLE_NOT_FOUND", "Rol bulunamadı.", 404));

        if (SUPER_ADMIN.equals(role.getCode())) {
            throw new EkokoyException("SUPER_ADMIN_INVITE_FORBIDDEN",
                    "SUPER_ADMIN rolü davet ile atanamaz.", 403);
        }

        Property property = null;
        if (request.propertyId() != null) {
            property = propertyRepository.findById(request.propertyId())
                    .orElseThrow(() -> new EkokoyException("PROPERTY_NOT_FOUND", "Konut bulunamadı.", 404));
        }

        UUID actorId = currentUserId();
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new EkokoyException("ACTOR_NOT_FOUND", "İşlemi yapan kullanıcı bulunamadı.", 404));

        String rawToken = generateRawToken();
        String tokenHash = sha256(rawToken);

        Invitation invitation = new Invitation();
        invitation.setEmail(request.email());
        invitation.setToken(tokenHash);
        invitation.setRole(role);
        invitation.setProperty(property);
        invitation.setCreatedBy(actor);
        invitation.setExpiresAt(Instant.now().plus(INVITATION_TTL_HOURS, ChronoUnit.HOURS));

        Invitation saved = invitationRepository.save(invitation);

        emailService.sendInvitationEmail(request.email(), rawToken, role.getDisplayName());
        auditLogService.log(actorId, "INVITATION_CREATE", "invitations", saved.getId(),
                "email=" + request.email() + ", role=" + role.getCode());

        return InvitationResponse.from(saved);
    }

    /**
     * Token doğrular; rate limit kontrolü yapılır.
     * Ham token alınır, SHA-256'sı hesaplanarak DB'de aranır.
     */
    public VerifyInvitationResponse verifyToken(String rawToken) {
        String tokenHash = sha256(rawToken);
        rateLimiterService.checkAndConsume(tokenHash);

        Invitation invitation = invitationRepository.findByTokenWithRole(tokenHash)
                .orElseThrow(() -> new EkokoyException("INVALID_TOKEN", "Geçersiz veya bulunamayan davet bağlantısı.", 404));

        if (invitation.isUsed()) {
            throw new EkokoyException("TOKEN_ALREADY_USED", "Bu davet bağlantısı daha önce kullanılmıştır.", 410);
        }
        if (Instant.now().isAfter(invitation.getExpiresAt())) {
            throw new EkokoyException("TOKEN_EXPIRED", "Davet bağlantısı süresi dolmuştur.", 410);
        }

        return VerifyInvitationResponse.from(invitation);
    }

    /**
     * Davet tamamlama: kullanıcı kaydını oluşturur, rolü atar, daveti kullanıldı işaretler.
     */
    @Transactional
    public ApplicationResponse completeInvitation(CompleteInvitationRequest request) {
        String tokenHash = sha256(request.token());
        rateLimiterService.checkAndConsume(tokenHash);

        Invitation invitation = invitationRepository.findByTokenWithRole(tokenHash)
                .orElseThrow(() -> new EkokoyException("INVALID_TOKEN", "Geçersiz davet bağlantısı.", 404));

        if (invitation.isUsed()) {
            throw new EkokoyException("TOKEN_ALREADY_USED", "Bu davet bağlantısı daha önce kullanılmıştır.", 410);
        }
        if (Instant.now().isAfter(invitation.getExpiresAt())) {
            throw new EkokoyException("TOKEN_EXPIRED", "Davet bağlantısı süresi dolmuştur.", 410);
        }
        if (userRepository.existsByEmailAndIsDeletedFalse(invitation.getEmail())) {
            throw new EkokoyException("EMAIL_EXISTS", "Bu e-posta adresi zaten kayıtlı.", 422);
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(invitation.getEmail());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.active);
        user.setApprovedAt(Instant.now());
        User savedUser = userRepository.save(user);

        UserRole userRole = new UserRole(savedUser, invitation.getRole(), invitation.getCreatedBy());
        userRoleRepository.save(userRole);

        invitation.setUsed(true);
        invitation.setUsedAt(Instant.now());
        invitationRepository.save(invitation);

        auditLogService.log(null, "INVITATION_COMPLETE", "users", savedUser.getId(),
                "email=" + savedUser.getEmail() + ", role=" + invitation.getRole().getCode()
                        + ", invitationId=" + invitation.getId());

        return ApplicationResponse.from(savedUser);
    }

    /**
     * Mevcut daveti yeniden gönderir; eski token silinir, yeni token oluşturulur.
     */
    @Transactional
    public InvitationResponse resendInvitation(UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new EkokoyException("INVITATION_NOT_FOUND", "Davet bulunamadı.", 404));

        if (invitation.isUsed()) {
            throw new EkokoyException("INVITATION_ALREADY_USED", "Bu davet zaten kullanılmıştır.", 422);
        }

        UUID actorId = currentUserId();

        String rawToken = generateRawToken();
        String tokenHash = sha256(rawToken);
        invitation.setToken(tokenHash);
        invitation.setExpiresAt(Instant.now().plus(INVITATION_TTL_HOURS, ChronoUnit.HOURS));
        invitationRepository.save(invitation);

        emailService.sendInvitationEmail(invitation.getEmail(), rawToken, invitation.getRole().getDisplayName());
        auditLogService.log(actorId, "INVITATION_RESEND", "invitations", invitationId,
                "email=" + invitation.getEmail());

        return InvitationResponse.from(invitation);
    }

    private String generateRawToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algoritması desteklenmiyor", e);
        }
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString((String) auth.getPrincipal());
    }
}

package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.user.dto.ApplicationResponse;
import com.ekokoy.portal.user.dto.ApplyRequest;
import com.ekokoy.portal.user.dto.UpdateApplicationRequest;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserStatus;
import com.ekokoy.portal.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public ApplicationService(UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               EmailService emailService,
                               AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    /**
     * Yeni üyelik başvurusu oluşturur (public endpoint).
     * E-posta mükerrerlik kontrolü yapılır; başvuru status=pending olarak kaydedilir.
     */
    @Transactional
    public ApplicationResponse apply(ApplyRequest request) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.email())) {
            throw new EkokoyException("EMAIL_EXISTS", "Bu e-posta adresi zaten kayıtlı.", 422);
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.pending);

        User saved = userRepository.save(user);
        auditLogService.log(null, "USER_APPLY", "users", saved.getId(),
                "email=" + saved.getEmail());

        return ApplicationResponse.from(saved);
    }

    /**
     * Tüm başvuruları listeler (pending + diğer statüler).
     * Silinmiş kullanıcılar hariç tutulur.
     */
    public List<ApplicationResponse> listApplications() {
        return userRepository.findAllByIsDeletedFalse().stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    /**
     * Başvuruyu onayla / reddet / bilgi iste.
     * action: approve | reject | request_info
     */
    @Transactional
    public ApplicationResponse updateApplication(UUID applicationId, UpdateApplicationRequest request) {
        User applicant = userRepository.findById(applicationId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new EkokoyException("USER_NOT_FOUND", "Başvuru bulunamadı.", 404));

        UUID actorId = currentUserId();
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new EkokoyException("ACTOR_NOT_FOUND", "İşlemi yapan kullanıcı bulunamadı.", 404));

        switch (request.action()) {
            case "approve" -> {
                if (applicant.getStatus() != UserStatus.pending) {
                    throw new EkokoyException("INVALID_STATUS",
                            "Sadece beklemedeki başvurular onaylanabilir.", 422);
                }
                applicant.setStatus(UserStatus.active);
                applicant.setApprovedAt(Instant.now());
                applicant.setApprovedBy(actor);
                emailService.sendApprovalEmail(applicant.getEmail(), applicant.getFirstName());
                auditLogService.log(actorId, "APPLICATION_APPROVE", "users", applicationId,
                        "email=" + applicant.getEmail());
            }
            case "reject" -> {
                applicant.setStatus(UserStatus.inactive);
                emailService.sendRejectionEmail(applicant.getEmail(), applicant.getFirstName(), request.message());
                auditLogService.log(actorId, "APPLICATION_REJECT", "users", applicationId,
                        "email=" + applicant.getEmail() + ", reason=" + request.message());
            }
            case "request_info" -> {
                if (request.message() == null || request.message().isBlank()) {
                    throw new EkokoyException("MESSAGE_REQUIRED",
                            "Bilgi talebinde mesaj zorunludur.", 422);
                }
                emailService.sendRequestInfoEmail(applicant.getEmail(), applicant.getFirstName(), request.message());
                auditLogService.log(actorId, "APPLICATION_REQUEST_INFO", "users", applicationId,
                        "email=" + applicant.getEmail() + ", message=" + request.message());
            }
            default -> throw new EkokoyException("INVALID_ACTION", "Geçersiz aksiyon.", 400);
        }

        return ApplicationResponse.from(userRepository.save(applicant));
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString((String) auth.getPrincipal());
    }
}

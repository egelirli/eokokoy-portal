package com.ekokoy.portal.announcement.service;

import com.ekokoy.portal.announcement.dto.*;
import com.ekokoy.portal.announcement.entity.*;
import com.ekokoy.portal.announcement.repository.*;
import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AnnouncementService {

    private static final int MAX_IMAGES = 5;
    private static final int MAX_DOCUMENTS = 3;
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementAttachmentRepository attachmentRepository;
    private final AnnouncementReadRepository readRepository;
    private final AnnouncementTargetRepository targetRepository;
    private final UserRepository userRepository;
    private final AnnouncementNotificationService notificationService;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               AnnouncementAttachmentRepository attachmentRepository,
                               AnnouncementReadRepository readRepository,
                               AnnouncementTargetRepository targetRepository,
                               UserRepository userRepository,
                               AnnouncementNotificationService notificationService) {
        this.announcementRepository = announcementRepository;
        this.attachmentRepository = attachmentRepository;
        this.readRepository = readRepository;
        this.targetRepository = targetRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /** Herkese açık yayınlanmış duyuruları listeler. */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getPublicAnnouncements() {
        return announcementRepository.findPublishedPublic()
                .stream().map(AnnouncementResponse::from).toList();
    }

    /** Yayınlanmış tüm duyuruları listeler (authenticated). */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAnnouncements() {
        return announcementRepository.findAllPublished()
                .stream().map(AnnouncementResponse::from).toList();
    }

    /** Tek duyuruyu getirir ve okunma kaydı oluşturur. */
    @Transactional
    public AnnouncementResponse getAnnouncement(UUID id) {
        Announcement announcement = requirePublished(id);
        UUID userId = currentUserId();
        if (!readRepository.existsByAnnouncementIdAndUserId(id, userId)) {
            User user = userRepository.getReferenceById(userId);
            AnnouncementRead read = new AnnouncementRead();
            read.setAnnouncement(announcement);
            read.setUser(user);
            read.setReadAt(Instant.now());
            readRepository.save(read);
        }
        return AnnouncementResponse.from(announcement);
    }

    /** Draft duyuru oluşturur. */
    @Transactional
    public AnnouncementResponse createAnnouncement(CreateAnnouncementRequest req) {
        User creator = userRepository.getReferenceById(currentUserId());
        validateTargets(req.targetType(), req.targets());

        Announcement ann = new Announcement();
        ann.setTitle(req.title());
        ann.setBody(req.body());
        ann.setPriority(req.priority());
        ann.setPublic(req.isPublic());
        ann.setTargetType(req.targetType());
        ann.setScheduledAt(req.scheduledAt());
        ann.setExpiresAt(req.expiresAt());
        ann.setCreatedBy(creator);

        Announcement saved = announcementRepository.save(ann);
        saveTargets(saved, req.targets());
        return AnnouncementResponse.from(announcementRepository.findByIdWithDetails(saved.getId()).orElseThrow());
    }

    /** Sadece draft duyuruyu günceller. */
    @Transactional
    public AnnouncementResponse updateAnnouncement(UUID id, UpdateAnnouncementRequest req) {
        Announcement ann = requireDraft(id);
        validateTargets(req.targetType(), req.targets());

        ann.setTitle(req.title());
        ann.setBody(req.body());
        ann.setPriority(req.priority());
        ann.setPublic(req.isPublic());
        ann.setTargetType(req.targetType());
        ann.setScheduledAt(req.scheduledAt());
        ann.setExpiresAt(req.expiresAt());

        targetRepository.deleteByAnnouncementId(id);
        announcementRepository.flush();
        saveTargets(ann, req.targets());

        announcementRepository.save(ann);
        return AnnouncementResponse.from(announcementRepository.findByIdWithDetails(id).orElseThrow());
    }

    /** Draft duyuruyu yayına alır. */
    @Transactional
    public AnnouncementResponse publish(UUID id) {
        Announcement ann = requireDraft(id);
        ann.setStatus(AnnouncementStatus.published);
        ann.setPublishedAt(Instant.now());
        announcementRepository.save(ann);
        notificationService.notifyPublished(ann);
        return AnnouncementResponse.from(announcementRepository.findByIdWithDetails(id).orElseThrow());
    }

    /** Yayınlanmış duyuruyu arşivler. */
    @Transactional
    public AnnouncementResponse archive(UUID id) {
        Announcement ann = announcementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> notFound(id));
        if (ann.getStatus() == AnnouncementStatus.archived) {
            throw new EkokoyException("ALREADY_ARCHIVED", "Duyuru zaten arşivlenmiş.", 422);
        }
        ann.setStatus(AnnouncementStatus.archived);
        ann.setArchivedAt(Instant.now());
        announcementRepository.save(ann);
        return AnnouncementResponse.from(announcementRepository.findByIdWithDetails(id).orElseThrow());
    }

    /** Sadece draft duyuruya ek dosya ekler. */
    @Transactional
    public AttachmentResponse addAttachment(UUID announcementId, AddAttachmentRequest req) {
        requireDraft(announcementId);
        validateAttachmentLimits(announcementId, req);

        AnnouncementAttachment att = new AnnouncementAttachment();
        att.setAnnouncement(announcementRepository.getReferenceById(announcementId));
        att.setFileUrl(req.fileUrl());
        att.setFileName(req.fileName());
        att.setFileType(req.fileType());
        att.setFileSize(req.fileSize());
        att.setDisplayOrder(req.displayOrder());
        return AttachmentResponse.from(attachmentRepository.save(att));
    }

    /** Sadece draft duyurudan ek dosya siler. */
    @Transactional
    public void deleteAttachment(UUID announcementId, UUID attachmentId) {
        requireDraft(announcementId);
        AnnouncementAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EkokoyException("ATTACHMENT_NOT_FOUND", "Ek dosya bulunamadı.", 404));
        if (!att.getAnnouncement().getId().equals(announcementId)) {
            throw new EkokoyException("ATTACHMENT_NOT_FOUND", "Ek dosya bu duyuruya ait değil.", 404);
        }
        attachmentRepository.delete(att);
    }

    /** Duyurunun okunma durumunu listeler (admin). */
    @Transactional(readOnly = true)
    public List<ReadStatusResponse> getReadStatus(UUID announcementId) {
        announcementRepository.findById(announcementId)
                .orElseThrow(() -> notFound(announcementId));
        return readRepository.findByAnnouncementIdWithUser(announcementId)
                .stream()
                .map(r -> new ReadStatusResponse(
                        r.getUser().getId(),
                        r.getUser().getFirstName() + " " + r.getUser().getLastName(),
                        r.getUser().getEmail(),
                        r.getReadAt()
                ))
                .toList();
    }

    // ── Scheduler tarafından çağrılır ────────────────────────────────────────────

    /** Zamanı gelen draft duyuruları yayına alır. */
    @Transactional
    public void publishScheduledAnnouncements() {
        List<Announcement> due = announcementRepository.findDraftsDueForPublishing(Instant.now());
        for (Announcement ann : due) {
            ann.setStatus(AnnouncementStatus.published);
            ann.setPublishedAt(Instant.now());
            announcementRepository.save(ann);
            notificationService.notifyPublished(ann);
        }
    }

    /** Süresi dolan yayınlanmış duyuruları arşivler. */
    @Transactional
    public void archiveExpiredAnnouncements() {
        List<Announcement> expired = announcementRepository.findExpiredPublished(Instant.now());
        for (Announcement ann : expired) {
            ann.setStatus(AnnouncementStatus.archived);
            ann.setArchivedAt(Instant.now());
            announcementRepository.save(ann);
        }
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────────

    private Announcement requireDraft(UUID id) {
        Announcement ann = announcementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> notFound(id));
        if (ann.getStatus() != AnnouncementStatus.draft) {
            throw new EkokoyException("NOT_DRAFT", "Bu işlem yalnızca draft duyurular için geçerlidir.", 422);
        }
        return ann;
    }

    private Announcement requirePublished(UUID id) {
        Announcement ann = announcementRepository.findByIdWithDetails(id)
                .orElseThrow(() -> notFound(id));
        if (ann.getStatus() != AnnouncementStatus.published) {
            throw new EkokoyException("NOT_PUBLISHED", "Duyuru yayınlanmamış.", 404);
        }
        return ann;
    }

    private void validateTargets(AnnouncementTargetType targetType, List<AnnouncementTargetRequest> targets) {
        if (targetType == AnnouncementTargetType.property_based || targetType == AnnouncementTargetType.role_based) {
            if (targets == null || targets.isEmpty()) {
                throw new EkokoyException("TARGET_REQUIRED",
                        "Bu hedefleme türü için en az bir hedef seçilmelidir.", 422);
            }
        }
    }

    private void saveTargets(Announcement ann, List<AnnouncementTargetRequest> targets) {
        if (targets == null) return;
        for (AnnouncementTargetRequest req : targets) {
            AnnouncementTarget t = new AnnouncementTarget();
            t.setAnnouncement(ann);
            t.setTargetEntityType(req.targetEntityType());
            t.setTargetId(req.targetId());
            targetRepository.save(t);
        }
    }

    private void validateAttachmentLimits(UUID announcementId, AddAttachmentRequest req) {
        if (req.fileType() == AttachmentFileType.image) {
            long count = attachmentRepository.countByAnnouncementIdAndFileType(announcementId, AttachmentFileType.image);
            if (count >= MAX_IMAGES) {
                throw new EkokoyException("IMAGE_LIMIT_EXCEEDED",
                        "Bir duyuruya en fazla " + MAX_IMAGES + " fotoğraf eklenebilir.", 422);
            }
            if (req.fileSize() != null && req.fileSize() > MAX_IMAGE_BYTES) {
                throw new EkokoyException("FILE_TOO_LARGE",
                        "Fotoğraf boyutu 5 MB'ı aşamaz.", 422);
            }
        } else {
            long count = attachmentRepository.countByAnnouncementIdAndFileType(announcementId, AttachmentFileType.document);
            if (count >= MAX_DOCUMENTS) {
                throw new EkokoyException("DOCUMENT_LIMIT_EXCEEDED",
                        "Bir duyuruya en fazla " + MAX_DOCUMENTS + " belge eklenebilir.", 422);
            }
            if (req.fileSize() != null && req.fileSize() > MAX_DOCUMENT_BYTES) {
                throw new EkokoyException("FILE_TOO_LARGE",
                        "Belge boyutu 10 MB'ı aşamaz.", 422);
            }
        }
    }

    private EkokoyException notFound(UUID id) {
        return new EkokoyException("ANNOUNCEMENT_NOT_FOUND", "Duyuru bulunamadı: " + id, 404);
    }

    private UUID currentUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}

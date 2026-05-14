package com.ekokoy.portal.announcement.service;

import com.ekokoy.portal.announcement.dto.*;
import com.ekokoy.portal.announcement.entity.*;
import com.ekokoy.portal.announcement.repository.*;
import com.ekokoy.portal.common.EkokoyException;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private AnnouncementAttachmentRepository attachmentRepository;
    @Mock private AnnouncementReadRepository readRepository;
    @Mock private AnnouncementTargetRepository targetRepository;
    @Mock private UserRepository userRepository;
    @Mock private AnnouncementNotificationService notificationService;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private AnnouncementService announcementService;

    private final UUID userId = UUID.randomUUID();
    private final UUID announcementId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn(userId.toString());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── getPublicAnnouncements ───────────────────────────────────────────────────

    @Test
    void should_return_public_published_announcements() {
        Announcement ann = makeDraftAnnouncement();
        ann.setStatus(AnnouncementStatus.published);
        ann.setPublic(true);
        when(announcementRepository.findPublishedPublic()).thenReturn(List.of(ann));

        List<AnnouncementResponse> result = announcementService.getPublicAnnouncements();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPublic()).isTrue();
    }

    // ── getAnnouncement ──────────────────────────────────────────────────────────

    @Test
    void should_create_read_record_when_first_time_viewed() {
        Announcement ann = makeDraftAnnouncement();
        ann.setStatus(AnnouncementStatus.published);
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));
        when(readRepository.existsByAnnouncementIdAndUserId(announcementId, userId)).thenReturn(false);
        when(userRepository.getReferenceById(userId)).thenReturn(makeUser());
        when(readRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        announcementService.getAnnouncement(announcementId);

        verify(readRepository).save(any(AnnouncementRead.class));
    }

    @Test
    void should_not_create_duplicate_read_record() {
        Announcement ann = makeDraftAnnouncement();
        ann.setStatus(AnnouncementStatus.published);
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));
        when(readRepository.existsByAnnouncementIdAndUserId(announcementId, userId)).thenReturn(true);

        announcementService.getAnnouncement(announcementId);

        verify(readRepository, never()).save(any());
    }

    @Test
    void should_throw_when_announcement_not_published() {
        Announcement ann = makeDraftAnnouncement();
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));

        assertThatThrownBy(() -> announcementService.getAnnouncement(announcementId))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("NOT_PUBLISHED");
    }

    // ── createAnnouncement ───────────────────────────────────────────────────────

    @Test
    void should_create_draft_announcement() {
        User creator = makeUser();
        Announcement saved = makeDraftAnnouncement();
        when(userRepository.getReferenceById(userId)).thenReturn(creator);
        when(announcementRepository.save(any())).thenReturn(saved);
        when(announcementRepository.findByIdWithDetails(any())).thenReturn(Optional.of(saved));

        CreateAnnouncementRequest req = new CreateAnnouncementRequest(
                "Başlık", "Gövde", AnnouncementPriority.normal,
                false, AnnouncementTargetType.all, null, null, null
        );

        AnnouncementResponse result = announcementService.createAnnouncement(req);

        assertThat(result).isNotNull();
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    void should_throw_when_property_based_without_targets() {
        when(userRepository.getReferenceById(userId)).thenReturn(makeUser());

        CreateAnnouncementRequest req = new CreateAnnouncementRequest(
                "Başlık", "Gövde", AnnouncementPriority.normal,
                false, AnnouncementTargetType.property_based, null, null, List.of()
        );

        assertThatThrownBy(() -> announcementService.createAnnouncement(req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("TARGET_REQUIRED");
    }

    @Test
    void should_throw_when_role_based_without_targets() {
        when(userRepository.getReferenceById(userId)).thenReturn(makeUser());

        CreateAnnouncementRequest req = new CreateAnnouncementRequest(
                "Başlık", "Gövde", AnnouncementPriority.normal,
                false, AnnouncementTargetType.role_based, null, null, null
        );

        assertThatThrownBy(() -> announcementService.createAnnouncement(req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("TARGET_REQUIRED");
    }

    // ── updateAnnouncement ───────────────────────────────────────────────────────

    @Test
    void should_update_draft_announcement() {
        Announcement ann = makeDraftAnnouncement();
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));
        when(announcementRepository.save(any())).thenReturn(ann);
        doNothing().when(announcementRepository).flush();
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));

        UpdateAnnouncementRequest req = new UpdateAnnouncementRequest(
                "Yeni Başlık", "Yeni Gövde", AnnouncementPriority.important,
                true, AnnouncementTargetType.all, null, null, null
        );

        AnnouncementResponse result = announcementService.updateAnnouncement(announcementId, req);

        assertThat(result).isNotNull();
        verify(targetRepository).deleteByAnnouncementId(announcementId);
    }

    @Test
    void should_throw_when_updating_published_announcement() {
        Announcement ann = makeDraftAnnouncement();
        ann.setStatus(AnnouncementStatus.published);
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));

        UpdateAnnouncementRequest req = new UpdateAnnouncementRequest(
                "Başlık", "Gövde", AnnouncementPriority.normal,
                false, AnnouncementTargetType.all, null, null, null
        );

        assertThatThrownBy(() -> announcementService.updateAnnouncement(announcementId, req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("NOT_DRAFT");
    }

    // ── publish ──────────────────────────────────────────────────────────────────

    @Test
    void should_publish_draft_announcement() {
        Announcement ann = makeDraftAnnouncement();
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));
        when(announcementRepository.save(any())).thenReturn(ann);

        announcementService.publish(announcementId);

        assertThat(ann.getStatus()).isEqualTo(AnnouncementStatus.published);
        assertThat(ann.getPublishedAt()).isNotNull();
        verify(notificationService).notifyPublished(ann);
    }

    @Test
    void should_throw_when_publishing_already_published() {
        Announcement ann = makeDraftAnnouncement();
        ann.setStatus(AnnouncementStatus.published);
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));

        assertThatThrownBy(() -> announcementService.publish(announcementId))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("NOT_DRAFT");
    }

    // ── archive ──────────────────────────────────────────────────────────────────

    @Test
    void should_archive_published_announcement() {
        Announcement ann = makeDraftAnnouncement();
        ann.setStatus(AnnouncementStatus.published);
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));
        when(announcementRepository.save(any())).thenReturn(ann);

        announcementService.archive(announcementId);

        assertThat(ann.getStatus()).isEqualTo(AnnouncementStatus.archived);
        assertThat(ann.getArchivedAt()).isNotNull();
    }

    @Test
    void should_throw_when_archiving_already_archived() {
        Announcement ann = makeDraftAnnouncement();
        ann.setStatus(AnnouncementStatus.archived);
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));

        assertThatThrownBy(() -> announcementService.archive(announcementId))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("ALREADY_ARCHIVED");
    }

    // ── addAttachment ─────────────────────────────────────────────────────────────

    @Test
    void should_add_image_attachment_to_draft() {
        Announcement ann = makeDraftAnnouncement();
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));
        when(attachmentRepository.countByAnnouncementIdAndFileType(announcementId, AttachmentFileType.image)).thenReturn(0L);
        when(announcementRepository.getReferenceById(announcementId)).thenReturn(ann);
        AnnouncementAttachment saved = new AnnouncementAttachment();
        saved.setFileUrl("https://minio/test.jpg");
        saved.setFileType(AttachmentFileType.image);
        when(attachmentRepository.save(any())).thenReturn(saved);

        AddAttachmentRequest req = new AddAttachmentRequest(
                "https://minio/test.jpg", "test.jpg", AttachmentFileType.image, 1024 * 1024, 0
        );

        AttachmentResponse result = announcementService.addAttachment(announcementId, req);

        assertThat(result).isNotNull();
    }

    @Test
    void should_throw_when_image_limit_exceeded() {
        Announcement ann = makeDraftAnnouncement();
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));
        when(attachmentRepository.countByAnnouncementIdAndFileType(announcementId, AttachmentFileType.image)).thenReturn(5L);

        AddAttachmentRequest req = new AddAttachmentRequest(
                "https://minio/test.jpg", "test.jpg", AttachmentFileType.image, 100, 0
        );

        assertThatThrownBy(() -> announcementService.addAttachment(announcementId, req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("IMAGE_LIMIT_EXCEEDED");
    }

    @Test
    void should_throw_when_image_too_large() {
        Announcement ann = makeDraftAnnouncement();
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));
        when(attachmentRepository.countByAnnouncementIdAndFileType(announcementId, AttachmentFileType.image)).thenReturn(0L);

        AddAttachmentRequest req = new AddAttachmentRequest(
                "https://minio/test.jpg", "test.jpg", AttachmentFileType.image,
                6 * 1024 * 1024, 0  // 6 MB > limit
        );

        assertThatThrownBy(() -> announcementService.addAttachment(announcementId, req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("FILE_TOO_LARGE");
    }

    @Test
    void should_throw_when_document_limit_exceeded() {
        Announcement ann = makeDraftAnnouncement();
        when(announcementRepository.findByIdWithDetails(announcementId)).thenReturn(Optional.of(ann));
        when(attachmentRepository.countByAnnouncementIdAndFileType(announcementId, AttachmentFileType.document)).thenReturn(3L);

        AddAttachmentRequest req = new AddAttachmentRequest(
                "https://minio/doc.pdf", "doc.pdf", AttachmentFileType.document, 100, 0
        );

        assertThatThrownBy(() -> announcementService.addAttachment(announcementId, req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("DOCUMENT_LIMIT_EXCEEDED");
    }

    // ── scheduler ────────────────────────────────────────────────────────────────

    @Test
    void should_publish_scheduled_announcements() {
        Announcement ann = makeDraftAnnouncement();
        ann.setScheduledAt(Instant.now().minusSeconds(60));
        when(announcementRepository.findDraftsDueForPublishing(any())).thenReturn(List.of(ann));
        when(announcementRepository.save(any())).thenReturn(ann);

        announcementService.publishScheduledAnnouncements();

        assertThat(ann.getStatus()).isEqualTo(AnnouncementStatus.published);
        assertThat(ann.getPublishedAt()).isNotNull();
        verify(notificationService).notifyPublished(ann);
    }

    @Test
    void should_archive_expired_announcements() {
        Announcement ann = makeDraftAnnouncement();
        ann.setStatus(AnnouncementStatus.published);
        ann.setExpiresAt(Instant.now().minusSeconds(60));
        when(announcementRepository.findExpiredPublished(any())).thenReturn(List.of(ann));
        when(announcementRepository.save(any())).thenReturn(ann);

        announcementService.archiveExpiredAnnouncements();

        assertThat(ann.getStatus()).isEqualTo(AnnouncementStatus.archived);
        assertThat(ann.getArchivedAt()).isNotNull();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private Announcement makeDraftAnnouncement() {
        Announcement ann = new Announcement();
        // Set ID via reflection won't work easily, mock via repository instead
        ann.setTitle("Test Duyuru");
        ann.setBody("Test gövde");
        ann.setPriority(AnnouncementPriority.normal);
        ann.setTargetType(AnnouncementTargetType.all);
        ann.setCreatedBy(makeUser());
        return ann;
    }

    private User makeUser() {
        User u = new User();
        u.setId(userId);
        u.setFirstName("Test");
        u.setLastName("Admin");
        u.setEmail("admin@ekokoy.com");
        u.setStatus(UserStatus.active);
        return u;
    }
}

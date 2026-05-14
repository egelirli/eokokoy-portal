package com.ekokoy.portal.storage.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.config.MinioProperties;
import com.ekokoy.portal.storage.dto.ConfirmUploadRequest;
import com.ekokoy.portal.storage.dto.FileResponse;
import com.ekokoy.portal.storage.dto.UploadUrlRequest;
import com.ekokoy.portal.storage.dto.UploadUrlResponse;
import com.ekokoy.portal.storage.entity.FileType;
import com.ekokoy.portal.storage.entity.StoredFile;
import com.ekokoy.portal.storage.repository.StoredFileRepository;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserStatus;
import com.ekokoy.portal.user.repository.UserRepository;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StorageServiceTest {

    @Mock private StoredFileRepository fileRepository;
    @Mock private UserRepository userRepository;
    @Mock private MinioClient minioClient;
    @Mock private MinioProperties minioProperties;
    @Mock private ImageProcessingService imageProcessingService;

    private StorageService storageService;

    private final UUID userId = UUID.randomUUID();
    private final String PRIVATE_BUCKET = "ekokoy-private";
    private final String ADMIN_BUCKET = "ekokoy-admin";
    private final String PUBLIC_BUCKET = "ekokoy-public";
    private final String PROFILES_BUCKET = "ekokoy-profiles";

    @BeforeEach
    void setUp() {
        storageService = new StorageService(
                fileRepository, userRepository, minioClient, minioProperties, imageProcessingService);

        when(minioProperties.getPublicBucket()).thenReturn(PUBLIC_BUCKET);
        when(minioProperties.getPrivateBucket()).thenReturn(PRIVATE_BUCKET);
        when(minioProperties.getAdminBucket()).thenReturn(ADMIN_BUCKET);
        when(minioProperties.getProfilesBucket()).thenReturn(PROFILES_BUCKET);

        setAuthentication(userId, List.of("ROLE_EV_SAHIBI"));
    }

    // ── generateUploadUrl ────────────────────────────────────────────────────────

    @Test
    void should_return_upload_url_when_valid_request() throws Exception {
        when(userRepository.getReferenceById(userId)).thenReturn(makeUser(userId));
        when(fileRepository.save(any())).thenAnswer(inv -> {
            StoredFile f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://minio/presigned");

        UploadUrlRequest req = new UploadUrlRequest(
                PRIVATE_BUCKET, "announcements", "foto.jpg", "image/jpeg", 1024L);

        UploadUrlResponse res = storageService.generateUploadUrl(req);

        assertThat(res.presignedUrl()).isEqualTo("http://minio/presigned");
        assertThat(res.fileId()).isNotNull();
        assertThat(res.objectKey()).contains("announcements/");
        verify(fileRepository).save(any(StoredFile.class));
    }

    @Test
    void should_reject_when_file_size_exceeds_image_limit() {
        UploadUrlRequest req = new UploadUrlRequest(
                PRIVATE_BUCKET, "photos", "big.jpg", "image/jpeg", 11L * 1024 * 1024);

        assertThatThrownBy(() -> storageService.generateUploadUrl(req))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    void should_reject_when_file_size_exceeds_document_limit() {
        UploadUrlRequest req = new UploadUrlRequest(
                PRIVATE_BUCKET, "docs", "big.pdf", "application/pdf", 21L * 1024 * 1024);

        assertThatThrownBy(() -> storageService.generateUploadUrl(req))
                .isInstanceOf(EkokoyException.class)
                .hasMessageContaining("20 MB");
    }

    @Test
    void should_reject_when_invalid_mime_type() {
        UploadUrlRequest req = new UploadUrlRequest(
                PRIVATE_BUCKET, "exec", "virus.exe", "application/x-msdownload", 1024L);

        assertThatThrownBy(() -> storageService.generateUploadUrl(req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("INVALID_MIME_TYPE");
    }

    @Test
    void should_reject_when_invalid_bucket() {
        UploadUrlRequest req = new UploadUrlRequest(
                "unknown-bucket", "m", "f.jpg", "image/jpeg", 1024L);

        assertThatThrownBy(() -> storageService.generateUploadUrl(req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("INVALID_BUCKET");
    }

    @Test
    void should_reject_admin_bucket_when_not_admin() {
        UploadUrlRequest req = new UploadUrlRequest(
                ADMIN_BUCKET, "admin", "f.pdf", "application/pdf", 1024L);

        assertThatThrownBy(() -> storageService.generateUploadUrl(req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("FORBIDDEN");
    }

    // ── confirmUpload ────────────────────────────────────────────────────────────

    @Test
    void should_confirm_and_return_file_when_no_duplicate() throws Exception {
        StoredFile pending = makePendingFile(userId, PRIVATE_BUCKET);
        when(fileRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(fakeGetObjectResponse("image/jpeg content".getBytes()));
        when(fileRepository.findByChecksumAndIsConfirmedTrueAndIsDeletedFalse(anyString()))
                .thenReturn(Optional.empty());
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://minio/get-url");

        String checksum = "a".repeat(64);
        FileResponse res = storageService.confirmUpload(pending.getId(), new ConfirmUploadRequest(checksum));

        assertThat(res.isConfirmed()).isTrue();
        verify(imageProcessingService).processAsync(pending.getId());
    }

    @Test
    void should_detect_duplicate_when_checksum_matches() throws Exception {
        StoredFile pending = makePendingFile(userId, PRIVATE_BUCKET);
        StoredFile existing = makeConfirmedFile(userId, PRIVATE_BUCKET);
        String checksum = "b".repeat(64);

        when(fileRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(fakeGetObjectResponse("image/jpeg content".getBytes()));
        when(fileRepository.findByChecksumAndIsConfirmedTrueAndIsDeletedFalse(checksum))
                .thenReturn(Optional.of(existing));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://minio/existing-url");

        FileResponse res = storageService.confirmUpload(pending.getId(), new ConfirmUploadRequest(checksum));

        assertThat(res.id()).isEqualTo(existing.getId());
        assertThat(pending.isDeleted()).isTrue();
        verify(imageProcessingService, never()).processAsync(any());
    }

    @Test
    void should_reject_confirm_when_not_owner() {
        UUID otherUserId = UUID.randomUUID();
        StoredFile pending = makePendingFile(otherUserId, PRIVATE_BUCKET);
        when(fileRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> storageService.confirmUpload(
                pending.getId(), new ConfirmUploadRequest("a".repeat(64))))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("FORBIDDEN");
    }

    // ── deleteFile ───────────────────────────────────────────────────────────────

    @Test
    void should_soft_delete_when_owner() {
        StoredFile file = makeConfirmedFile(userId, PRIVATE_BUCKET);
        when(fileRepository.findById(file.getId())).thenReturn(Optional.of(file));
        when(userRepository.getReferenceById(userId)).thenReturn(makeUser(userId));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        storageService.deleteFile(file.getId());

        assertThat(file.isDeleted()).isTrue();
        assertThat(file.getDeletedAt()).isNotNull();
    }

    @Test
    void should_reject_delete_when_not_owner_and_not_admin() {
        UUID ownerUserId = UUID.randomUUID();
        StoredFile file = makeConfirmedFile(ownerUserId, PRIVATE_BUCKET);
        when(fileRepository.findById(file.getId())).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> storageService.deleteFile(file.getId()))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("FORBIDDEN");
    }

    @Test
    void should_allow_admin_to_delete_any_file() {
        setAuthentication(userId, List.of("ROLE_SUPER_ADMIN"));
        UUID ownerUserId = UUID.randomUUID();
        StoredFile file = makeConfirmedFile(ownerUserId, PRIVATE_BUCKET);
        when(fileRepository.findById(file.getId())).thenReturn(Optional.of(file));
        when(userRepository.getReferenceById(userId)).thenReturn(makeUser(userId));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        storageService.deleteFile(file.getId());

        assertThat(file.isDeleted()).isTrue();
    }

    // ── Bucket erişim matrisi ────────────────────────────────────────────────────

    @Test
    void should_deny_admin_bucket_read_for_regular_user() {
        StoredFile file = makeConfirmedFile(userId, ADMIN_BUCKET);
        when(fileRepository.findById(file.getId())).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> storageService.getFile(file.getId()))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("FORBIDDEN");
    }

    @Test
    void should_allow_admin_bucket_read_for_admin() {
        setAuthentication(userId, List.of("ROLE_YONETIM_KURULU"));
        StoredFile file = makeConfirmedFile(userId, ADMIN_BUCKET);
        when(fileRepository.findById(file.getId())).thenReturn(Optional.of(file));
        when(minioProperties.getEndpoint()).thenReturn("http://minio:9000");
        try {
            when(minioClient.getPresignedObjectUrl(any())).thenReturn("http://url");
        } catch (Exception ignored) {}

        assertThatNoException().isThrownBy(() -> storageService.getFile(file.getId()));
    }

    @Test
    void should_deny_profiles_bucket_read_for_other_user() {
        UUID ownerUserId = UUID.randomUUID();
        StoredFile file = makeConfirmedFile(ownerUserId, PROFILES_BUCKET);
        when(fileRepository.findById(file.getId())).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> storageService.getFile(file.getId()))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("FORBIDDEN");
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────────

    private void setAuthentication(UUID uid, List<String> roles) {
        var authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();
        var auth = new UsernamePasswordAuthenticationToken(uid.toString(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User makeUser(UUID id) {
        User u = new User();
        u.setId(id);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail("test@ekokoy.com");
        u.setStatus(UserStatus.active);
        return u;
    }

    private StoredFile makePendingFile(UUID uploaderId, String bucket) {
        StoredFile f = new StoredFile();
        f.setId(UUID.randomUUID());
        f.setBucket(bucket);
        f.setObjectKey("module/2025/01/" + UUID.randomUUID() + ".jpg");
        f.setOriginalName("foto.jpg");
        f.setMimeType("image/jpeg");
        f.setFileSize(1024L);
        f.setFileType(FileType.image);
        f.setUploadedBy(makeUser(uploaderId));
        f.setUploadedAt(Instant.now());
        f.setConfirmed(false);
        f.setDeleted(false);
        return f;
    }

    private StoredFile makeConfirmedFile(UUID uploaderId, String bucket) {
        StoredFile f = makePendingFile(uploaderId, bucket);
        f.setConfirmed(true);
        f.setChecksum("c".repeat(64));
        return f;
    }

    @SuppressWarnings("unchecked")
    private GetObjectResponse fakeGetObjectResponse(byte[] data) {
        return mock(GetObjectResponse.class, inv -> {
            if (inv.getMethod().getName().equals("read")) {
                byte[] buf = inv.getArgument(0);
                int len = Math.min(buf.length, data.length);
                System.arraycopy(data, 0, buf, 0, len);
                return len > 0 ? len : -1;
            }
            if (inv.getMethod().getName().equals("close")) return null;
            return new ByteArrayInputStream(data).read();
        });
    }
}

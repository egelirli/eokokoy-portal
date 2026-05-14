package com.ekokoy.portal.storage;

import com.ekokoy.portal.config.JwtUtil;
import com.ekokoy.portal.storage.entity.FileType;
import com.ekokoy.portal.storage.entity.StoredFile;
import com.ekokoy.portal.storage.repository.StoredFileRepository;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserStatus;
import com.ekokoy.portal.user.repository.UserRepository;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class FileControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @MockitoBean
    MinioClient minioClient;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private StoredFileRepository fileRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String userToken;
    private String adminToken;
    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() throws Exception {
        testUser = userRepository.findByEmailAndIsDeletedFalse("file-user@ekokoy.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setFirstName("File");
                    u.setLastName("User");
                    u.setEmail("file-user@ekokoy.com");
                    u.setPasswordHash(passwordEncoder.encode("TestPass1!"));
                    u.setStatus(UserStatus.active);
                    return userRepository.save(u);
                });

        adminUser = userRepository.findByEmailAndIsDeletedFalse("file-admin@ekokoy.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setFirstName("File");
                    u.setLastName("Admin");
                    u.setEmail("file-admin@ekokoy.com");
                    u.setPasswordHash(passwordEncoder.encode("TestPass1!"));
                    u.setStatus(UserStatus.active);
                    return userRepository.save(u);
                });

        userToken = jwtUtil.generateAccessToken(
                testUser.getId(), testUser.getEmail(),
                List.of("EV_SAHIBI"), List.of(), List.of());

        adminToken = jwtUtil.generateAccessToken(
                adminUser.getId(), adminUser.getEmail(),
                List.of("SUPER_ADMIN"), List.of(), List.of());

        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://minio:9000/presigned-url");
    }

    // ── 401 / Kimlik doğrulama ───────────────────────────────────────────────────

    @Test
    void should_return_401_when_no_token_on_upload_url() throws Exception {
        mockMvc.perform(post("/api/v1/files/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bucket":"ekokoy-private","module":"test","originalName":"a.jpg","mimeType":"image/jpeg","fileSize":1024}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_401_when_no_token_on_get_file() throws Exception {
        mockMvc.perform(get("/api/v1/files/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // ── Upload URL ───────────────────────────────────────────────────────────────

    @Test
    void should_return_upload_url_when_authenticated() throws Exception {
        mockMvc.perform(post("/api/v1/files/upload-url")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bucket": "ekokoy-private",
                                  "module": "announcements",
                                  "originalName": "photo.jpg",
                                  "mimeType": "image/jpeg",
                                  "fileSize": 2097152
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileId").isNotEmpty())
                .andExpect(jsonPath("$.data.presignedUrl").value("http://minio:9000/presigned-url"))
                .andExpect(jsonPath("$.data.objectKey").isNotEmpty());
    }

    @Test
    void should_reject_when_file_too_large() throws Exception {
        mockMvc.perform(post("/api/v1/files/upload-url")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bucket": "ekokoy-private",
                                  "module": "photos",
                                  "originalName": "big.jpg",
                                  "mimeType": "image/jpeg",
                                  "fileSize": 11534336
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void should_reject_when_invalid_mime_type() throws Exception {
        mockMvc.perform(post("/api/v1/files/upload-url")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bucket": "ekokoy-private",
                                  "module": "exec",
                                  "originalName": "virus.exe",
                                  "mimeType": "application/x-msdownload",
                                  "fileSize": 1024
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void should_return_403_when_regular_user_uploads_to_admin_bucket() throws Exception {
        mockMvc.perform(post("/api/v1/files/upload-url")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bucket": "ekokoy-admin",
                                  "module": "admin",
                                  "originalName": "doc.pdf",
                                  "mimeType": "application/pdf",
                                  "fileSize": 1024
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_allow_admin_to_upload_to_admin_bucket() throws Exception {
        mockMvc.perform(post("/api/v1/files/upload-url")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bucket": "ekokoy-admin",
                                  "module": "admin",
                                  "originalName": "doc.pdf",
                                  "mimeType": "application/pdf",
                                  "fileSize": 1024
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.presignedUrl").isNotEmpty());
    }

    // ── Confirm upload ───────────────────────────────────────────────────────────

    @Test
    void should_confirm_upload_successfully() throws Exception {
        // Önce bekleyen dosya kaydı oluştur
        StoredFile pending = createPendingFile(testUser, "ekokoy-private");
        String checksum = "a".repeat(64);

        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(mock(io.minio.GetObjectResponse.class, inv -> {
                    if (inv.getMethod().getName().equals("read")) {
                        byte[] b = inv.getArgument(0);
                        byte[] data = "fake jpeg data".getBytes();
                        int len = Math.min(b.length, data.length);
                        System.arraycopy(data, 0, b, 0, len);
                        return len > 0 ? len : -1;
                    }
                    return null;
                }));

        mockMvc.perform(post("/api/v1/files/" + pending.getId() + "/confirm")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checksum\":\"" + checksum + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isConfirmed").value(true));
    }

    // ── Get file ─────────────────────────────────────────────────────────────────

    @Test
    void should_return_404_when_file_not_found() throws Exception {
        mockMvc.perform(get("/api/v1/files/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_file_when_confirmed() throws Exception {
        StoredFile file = createConfirmedFile(testUser, "ekokoy-private");

        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://minio:9000/file-url");

        mockMvc.perform(get("/api/v1/files/" + file.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(file.getId().toString()))
                .andExpect(jsonPath("$.data.bucket").value("ekokoy-private"));
    }

    @Test
    void should_deny_admin_bucket_access_for_regular_user() throws Exception {
        StoredFile file = createConfirmedFile(adminUser, "ekokoy-admin");

        mockMvc.perform(get("/api/v1/files/" + file.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ── Soft delete ──────────────────────────────────────────────────────────────

    @Test
    void should_soft_delete_file_when_owner() throws Exception {
        StoredFile file = createConfirmedFile(testUser, "ekokoy-private");

        mockMvc.perform(delete("/api/v1/files/" + file.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        StoredFile deleted = fileRepository.findById(file.getId()).orElseThrow();
        assert deleted.isDeleted();
        assert deleted.getDeletedAt() != null;
    }

    @Test
    void should_deny_delete_when_not_owner() throws Exception {
        StoredFile file = createConfirmedFile(adminUser, "ekokoy-private");

        mockMvc.perform(delete("/api/v1/files/" + file.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────────

    private StoredFile createPendingFile(User owner, String bucket) {
        StoredFile f = new StoredFile();
        f.setBucket(bucket);
        f.setObjectKey("test/2025/01/" + UUID.randomUUID() + ".jpg");
        f.setOriginalName("test.jpg");
        f.setMimeType("image/jpeg");
        f.setFileSize(1024L);
        f.setFileType(FileType.image);
        f.setUploadedBy(owner);
        f.setUploadedAt(Instant.now());
        f.setConfirmed(false);
        f.setDeleted(false);
        return fileRepository.save(f);
    }

    private StoredFile createConfirmedFile(User owner, String bucket) {
        StoredFile f = createPendingFile(owner, bucket);
        f.setConfirmed(true);
        f.setChecksum("d".repeat(64));
        return fileRepository.save(f);
    }
}

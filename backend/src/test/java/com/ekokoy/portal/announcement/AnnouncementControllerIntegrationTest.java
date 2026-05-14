package com.ekokoy.portal.announcement;

import com.ekokoy.portal.config.JwtUtil;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserStatus;
import com.ekokoy.portal.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AnnouncementControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        User admin = userRepository.findByEmailAndIsDeletedFalse("ann-admin@ekokoy.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setFirstName("Ann");
                    u.setLastName("Admin");
                    u.setEmail("ann-admin@ekokoy.com");
                    u.setPasswordHash(passwordEncoder.encode("TestPass1!"));
                    u.setStatus(UserStatus.active);
                    return userRepository.save(u);
                });

        User regular = userRepository.findByEmailAndIsDeletedFalse("ann-user@ekokoy.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setFirstName("Ann");
                    u.setLastName("User");
                    u.setEmail("ann-user@ekokoy.com");
                    u.setPasswordHash(passwordEncoder.encode("TestPass1!"));
                    u.setStatus(UserStatus.active);
                    return userRepository.save(u);
                });

        adminToken = jwtUtil.generateAccessToken(
                admin.getId(), admin.getEmail(),
                List.of("SUPER_ADMIN"), List.of(), List.of()
        );

        userToken = jwtUtil.generateAccessToken(
                regular.getId(), regular.getEmail(),
                List.of("EV_SAHIBI"), List.of(), List.of()
        );
    }

    // ── Public endpoint ──────────────────────────────────────────────────────────

    @Test
    void should_return_empty_list_when_no_public_announcements() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── Authenticated list ───────────────────────────────────────────────────────

    @Test
    void should_return_401_when_listing_without_token() throws Exception {
        mockMvc.perform(get("/api/v1/announcements"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_announcements_when_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/announcements")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── Admin CRUD ───────────────────────────────────────────────────────────────

    @Test
    void should_return_403_when_regular_user_creates_announcement() throws Exception {
        mockMvc.perform(post("/api/v1/admin/announcements")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test",
                                  "body": "Gövde",
                                  "priority": "normal",
                                  "isPublic": false,
                                  "targetType": "all"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_create_draft_when_admin() throws Exception {
        mockMvc.perform(post("/api/v1/admin/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test Duyuru",
                                  "body": "Test gövde içeriği",
                                  "priority": "normal",
                                  "isPublic": false,
                                  "targetType": "all"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.title").value("Test Duyuru"));
    }

    @Test
    void should_complete_draft_publish_archive_flow() throws Exception {
        // 1. Draft oluştur
        String createResponse = mockMvc.perform(post("/api/v1/admin/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Akış Testi",
                                  "body": "Akış gövde",
                                  "priority": "important",
                                  "isPublic": true,
                                  "targetType": "all"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andReturn().getResponse().getContentAsString();

        String id = extractId(createResponse);

        // 2. Yayına al
        mockMvc.perform(patch("/api/v1/admin/announcements/" + id + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("published"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());

        // 3. Yayınlanmış duyuru authenticated listede görünüyor
        mockMvc.perform(get("/api/v1/announcements")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + id + "')]").exists());

        // 4. Public listede görünüyor (isPublic=true)
        mockMvc.perform(get("/api/v1/announcements/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + id + "')]").exists());

        // 5. Arşivle
        mockMvc.perform(patch("/api/v1/admin/announcements/" + id + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("archived"))
                .andExpect(jsonPath("$.data.archivedAt").isNotEmpty());
    }

    @Test
    void should_record_read_when_viewing_announcement() throws Exception {
        // Draft oluştur ve yayına al
        String createResponse = mockMvc.perform(post("/api/v1/admin/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Okunma Testi",
                                  "body": "Okunma gövde",
                                  "priority": "normal",
                                  "isPublic": false,
                                  "targetType": "all"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();

        String id = extractId(createResponse);

        mockMvc.perform(patch("/api/v1/admin/announcements/" + id + "/publish")
                .header("Authorization", "Bearer " + adminToken));

        // Kullanıcı duyuruyu okur
        mockMvc.perform(get("/api/v1/announcements/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Okunma durumu admin tarafından görülebilir
        mockMvc.perform(get("/api/v1/admin/announcements/" + id + "/read-status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void should_not_edit_published_announcement() throws Exception {
        // Draft oluştur ve yayına al
        String createResponse = mockMvc.perform(post("/api/v1/admin/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Düzenleme Testi",
                                  "body": "Gövde",
                                  "priority": "normal",
                                  "isPublic": false,
                                  "targetType": "all"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();

        String id = extractId(createResponse);

        mockMvc.perform(patch("/api/v1/admin/announcements/" + id + "/publish")
                .header("Authorization", "Bearer " + adminToken));

        // Yayınlanmış duyuruyu düzenlemeye çalış
        mockMvc.perform(put("/api/v1/admin/announcements/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Değiştirilmiş Başlık",
                                  "body": "Değiştirilmiş Gövde",
                                  "priority": "normal",
                                  "isPublic": false,
                                  "targetType": "all"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void should_reject_property_based_announcement_without_targets() throws Exception {
        mockMvc.perform(post("/api/v1/admin/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hedef Testi",
                                  "body": "Gövde",
                                  "priority": "normal",
                                  "isPublic": false,
                                  "targetType": "property_based",
                                  "targets": []
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void should_add_and_delete_attachment_on_draft() throws Exception {
        // Draft oluştur
        String createResponse = mockMvc.perform(post("/api/v1/admin/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Ek Dosya Testi",
                                  "body": "Gövde",
                                  "priority": "normal",
                                  "isPublic": false,
                                  "targetType": "all"
                                }
                                """))
                .andReturn().getResponse().getContentAsString();

        String id = extractId(createResponse);

        // Ek dosya ekle
        String attResponse = mockMvc.perform(post("/api/v1/admin/announcements/" + id + "/attachments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileUrl": "https://minio.example.com/test.jpg",
                                  "fileName": "test.jpg",
                                  "fileType": "image",
                                  "fileSize": 102400,
                                  "displayOrder": 0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileType").value("image"))
                .andReturn().getResponse().getContentAsString();

        String attachmentId = extractAttachmentId(attResponse);

        // Ek dosyayı sil
        mockMvc.perform(delete("/api/v1/admin/announcements/" + id + "/attachments/" + attachmentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private String extractId(String json) {
        String search = "\"id\":\"";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private String extractAttachmentId(String json) {
        // data.id field
        String search = "\"data\":{\"id\":\"";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}

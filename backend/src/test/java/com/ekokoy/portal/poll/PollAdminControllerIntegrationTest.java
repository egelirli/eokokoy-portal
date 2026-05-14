package com.ekokoy.portal.poll;

import com.ekokoy.portal.config.JwtUtil;
import com.ekokoy.portal.poll.entity.*;
import com.ekokoy.portal.poll.repository.PollRepository;
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

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PollAdminControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private PollRepository pollRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.findByEmailAndIsDeletedFalse("poll-admin@ekokoy.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setFirstName("Poll");
                    u.setLastName("Admin");
                    u.setEmail("poll-admin@ekokoy.com");
                    u.setPasswordHash(passwordEncoder.encode("TestPass1!"));
                    u.setStatus(UserStatus.active);
                    return userRepository.save(u);
                });

        User regularUser = userRepository.findByEmailAndIsDeletedFalse("poll-user@ekokoy.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setFirstName("Poll");
                    u.setLastName("User");
                    u.setEmail("poll-user@ekokoy.com");
                    u.setPasswordHash(passwordEncoder.encode("TestPass1!"));
                    u.setStatus(UserStatus.active);
                    return userRepository.save(u);
                });

        adminToken = jwtUtil.generateAccessToken(
                adminUser.getId(), adminUser.getEmail(),
                List.of("SUPER_ADMIN"), List.of(), List.of());

        userToken = jwtUtil.generateAccessToken(
                regularUser.getId(), regularUser.getEmail(),
                List.of("EV_SAHIBI"), List.of(), List.of());
    }

    // ── POST /api/v1/admin/polls ──────────────────────────────────────────────────

    @Test
    void should_create_poll_as_admin() throws Exception {
        String body = """
                {
                  "type": "vote",
                  "title": "Test Oylaması",
                  "isAnonymous": false,
                  "eligibleRoles": ["EV_SAHIBI"],
                  "startsAt": "2026-06-01T09:00:00Z",
                  "questions": [
                    {
                      "questionText": "Katılıyor musunuz?",
                      "questionType": "yes_no",
                      "isRequired": true,
                      "questionOrder": 0,
                      "options": [
                        {"optionText": "Evet", "optionOrder": 0},
                        {"optionText": "Hayır", "optionOrder": 1}
                      ]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/admin/polls")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.questions", hasSize(1)));
    }

    @Test
    void should_reject_poll_creation_for_non_admin() throws Exception {
        String body = """
                {
                  "type": "vote",
                  "title": "X",
                  "isAnonymous": false,
                  "eligibleRoles": ["EV_SAHIBI"],
                  "startsAt": "2026-06-01T09:00:00Z",
                  "questions": [{"questionText": "S?", "questionType": "yes_no",
                                 "isRequired": true, "questionOrder": 0, "options": []}]
                }
                """;

        mockMvc.perform(post("/api/v1/admin/polls")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /api/v1/admin/polls/:id/activate ────────────────────────────────────

    @Test
    void should_activate_draft_poll() throws Exception {
        Poll poll = createDraftPoll("Aktivasyon Anketi");

        mockMvc.perform(patch("/api/v1/admin/polls/" + poll.getId() + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"));
    }

    @Test
    void should_reject_activation_of_active_poll() throws Exception {
        Poll poll = createActivePoll("Zaten Aktif");

        mockMvc.perform(patch("/api/v1/admin/polls/" + poll.getId() + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── PATCH /api/v1/admin/polls/:id/close ──────────────────────────────────────

    @Test
    void should_close_active_poll() throws Exception {
        Poll poll = createActivePoll("Kapanacak Anket");

        mockMvc.perform(patch("/api/v1/admin/polls/" + poll.getId() + "/close")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("closed"));
    }

    // ── PATCH /api/v1/admin/polls/:id/cancel ─────────────────────────────────────

    @Test
    void should_cancel_poll_as_super_admin() throws Exception {
        Poll poll = createDraftPoll("İptal Anketi");

        mockMvc.perform(patch("/api/v1/admin/polls/" + poll.getId() + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));
    }

    // ── GET /api/v1/admin/polls ───────────────────────────────────────────────────

    @Test
    void should_list_all_polls_as_admin() throws Exception {
        createDraftPoll("Liste Anketi");

        mockMvc.perform(get("/api/v1/admin/polls")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── GET /api/v1/admin/polls/:id/results/full ─────────────────────────────────

    @Test
    void should_return_full_results() throws Exception {
        Poll poll = createActivePoll("Sonuç Anketi");

        mockMvc.perform(get("/api/v1/admin/polls/" + poll.getId() + "/results/full")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pollId").value(poll.getId().toString()))
                .andExpect(jsonPath("$.data.totalParticipants").isNumber());
    }

    // ── GET /api/v1/admin/polls/:id/participants ──────────────────────────────────

    @Test
    void should_return_participants_list() throws Exception {
        Poll poll = createActivePoll("Katılımcı Anketi");

        mockMvc.perform(get("/api/v1/admin/polls/" + poll.getId() + "/participants")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── GET /api/v1/polls ─────────────────────────────────────────────────────────

    @Test
    void should_list_eligible_active_polls_for_user() throws Exception {
        createActivePoll("Kullanıcı Anketi");

        mockMvc.perform(get("/api/v1/polls")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void should_reject_unauthenticated_poll_list() throws Exception {
        mockMvc.perform(get("/api/v1/polls"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/v1/polls/:id/respond ────────────────────────────────────────────

    @Test
    void should_reject_response_to_draft_poll() throws Exception {
        Poll poll = createDraftPoll("Taslak Anket");

        String body = """
                {"answers": []}
                """;

        mockMvc.perform(post("/api/v1/polls/" + poll.getId() + "/respond")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── PUT /api/v1/admin/polls/:id ───────────────────────────────────────────────

    @Test
    void should_update_draft_poll() throws Exception {
        Poll poll = createDraftPoll("Güncellenecek");

        String body = """
                {
                  "type": "survey",
                  "title": "Güncellendi",
                  "isAnonymous": true,
                  "eligibleRoles": ["EV_SAHIBI","KIRACI"],
                  "startsAt": "2026-07-01T09:00:00Z",
                  "questions": [
                    {"questionText": "Yeni soru?", "questionType": "text",
                     "isRequired": false, "questionOrder": 0, "options": []}
                  ]
                }
                """;

        mockMvc.perform(put("/api/v1/admin/polls/" + poll.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Güncellendi"))
                .andExpect(jsonPath("$.data.isAnonymous").value(true));
    }

    @Test
    void should_reject_update_of_active_poll() throws Exception {
        Poll poll = createActivePoll("Aktif - güncellenemez");

        String body = """
                {
                  "type": "vote",
                  "title": "X",
                  "isAnonymous": false,
                  "eligibleRoles": ["EV_SAHIBI"],
                  "startsAt": "2026-06-01T09:00:00Z",
                  "questions": [
                    {"questionText": "S?", "questionType": "yes_no",
                     "isRequired": true, "questionOrder": 0, "options": []}
                  ]
                }
                """;

        mockMvc.perform(put("/api/v1/admin/polls/" + poll.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private Poll createDraftPoll(String title) {
        Poll poll = new Poll();
        poll.setType(PollType.vote);
        poll.setTitle(title);
        poll.setStatus(PollStatus.draft);
        poll.setAnonymous(false);
        poll.setEligibleRoles(new String[]{"EV_SAHIBI"});
        poll.setStartsAt(Instant.now().plusSeconds(3600));
        poll.setCreatedBy(adminUser);
        return pollRepository.save(poll);
    }

    private Poll createActivePoll(String title) {
        Poll poll = new Poll();
        poll.setType(PollType.vote);
        poll.setTitle(title);
        poll.setStatus(PollStatus.active);
        poll.setAnonymous(false);
        poll.setEligibleRoles(new String[]{"EV_SAHIBI"});
        poll.setStartsAt(Instant.now().minusSeconds(60));
        poll.setCreatedBy(adminUser);
        return pollRepository.save(poll);
    }
}

package com.ekokoy.portal.user;

import com.ekokoy.portal.config.JwtUtil;
import com.ekokoy.portal.user.entity.*;
import com.ekokoy.portal.user.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PropertyControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private PropertyUserRepository propertyUserRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;

    private String adminToken;
    private UUID adminUserId;
    private UUID testPropertyId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        var adminUser = userRepository.findByEmailAndIsDeletedFalse("admin@ekokoy.com");
        adminUserId = adminUser.map(User::getId).orElse(UUID.randomUUID());

        adminToken = jwtUtil.generateAccessToken(
                adminUserId,
                List.of("SUPER_ADMIN"),
                List.of("PROPERTY_VIEW", "PROPERTY_MANAGE"),
                List.of()
        );

        // Seed verilerinden ilk konutu al
        testPropertyId = propertyRepository.findByNumber(1)
                .map(Property::getId)
                .orElseGet(() -> propertyRepository.findAll().stream()
                        .findFirst()
                        .map(Property::getId)
                        .orElseThrow());

        // Test kullanıcısı oluştur
        User testUser = new User();
        testUser.setFirstName("Test");
        testUser.setLastName("Sakin");
        testUser.setEmail("sakin-" + UUID.randomUUID() + "@ekokoy.com");
        testUser.setStatus(UserStatus.active);
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();
    }

    @Test
    void should_return_94_properties_when_admin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/properties")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(94));
    }

    @Test
    void should_return_property_detail_when_valid_id() throws Exception {
        mockMvc.perform(get("/api/v1/admin/properties/{id}", testPropertyId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.number").value(1))
                .andExpect(jsonPath("$.data.status").value("bos"))
                .andExpect(jsonPath("$.data.activeResidents").isArray());
    }

    @Test
    void should_return_401_without_token() throws Exception {
        mockMvc.perform(get("/api/v1/admin/properties"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_add_ev_sahibi_and_update_property_status() throws Exception {
        String requestBody = """
                {
                    "userId": "%s",
                    "relationType": "ev_sahibi",
                    "ownershipPercentage": 100.00,
                    "startDate": "%s"
                }
                """.formatted(testUserId, LocalDate.now());

        mockMvc.perform(post("/api/v1/admin/properties/{id}/residents", testPropertyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.relationType").value("ev_sahibi"));

        // Konut durumu sahipli olmalı
        mockMvc.perform(get("/api/v1/admin/properties/{id}", testPropertyId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data.status").value("sahipli"));
    }

    @Test
    void should_reject_second_kiraci() throws Exception {
        // İlk kiracıyı ekle
        String firstKiraciBody = """
                {
                    "userId": "%s",
                    "relationType": "kiraci",
                    "startDate": "%s"
                }
                """.formatted(testUserId, LocalDate.now());

        mockMvc.perform(post("/api/v1/admin/properties/{id}/residents", testPropertyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstKiraciBody))
                .andExpect(status().isCreated());

        // İkinci kullanıcı oluştur
        User secondUser = new User();
        secondUser.setFirstName("İkinci");
        secondUser.setLastName("Kiracı");
        secondUser.setEmail("kiraci2-" + UUID.randomUUID() + "@ekokoy.com");
        secondUser.setStatus(UserStatus.active);
        secondUser = userRepository.save(secondUser);

        // İkinci kiracıyı eklemeye çalış — 422 beklenir
        String secondKiraciBody = """
                {
                    "userId": "%s",
                    "relationType": "kiraci",
                    "startDate": "%s"
                }
                """.formatted(secondUser.getId(), LocalDate.now());

        mockMvc.perform(post("/api/v1/admin/properties/{id}/residents", testPropertyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondKiraciBody))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void should_reject_aile_bireyi_without_ev_sahibi() throws Exception {
        // Konutta ev sahibi yok, aile bireyi eklemeye çalış
        String requestBody = """
                {
                    "userId": "%s",
                    "relationType": "aile_bireyi",
                    "startDate": "%s"
                }
                """.formatted(testUserId, LocalDate.now());

        mockMvc.perform(post("/api/v1/admin/properties/{id}/residents", testPropertyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void should_end_relation_and_update_status_to_bos() throws Exception {
        // Önce ev sahibi ekle
        String addBody = """
                {
                    "userId": "%s",
                    "relationType": "ev_sahibi",
                    "ownershipPercentage": 100.00,
                    "startDate": "%s"
                }
                """.formatted(testUserId, LocalDate.now());

        String addResponse = mockMvc.perform(post("/api/v1/admin/properties/{id}/residents", testPropertyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // relation ID'yi parse et
        String relationId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(addResponse).path("data").path("id").asText();

        // İlişkiyi sonlandır
        mockMvc.perform(patch("/api/v1/admin/properties/{propertyId}/residents/{relationId}/end",
                        testPropertyId, relationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endDate").isNotEmpty());

        // Konut durumu boş olmalı
        mockMvc.perform(get("/api/v1/admin/properties/{id}", testPropertyId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data.status").value("bos"));
    }

    @Test
    void should_return_property_history() throws Exception {
        mockMvc.perform(get("/api/v1/admin/properties/{id}/history", testPropertyId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void should_return_user_properties() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/{id}/properties", testUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void should_return_my_properties_for_authenticated_user() throws Exception {
        String userToken = jwtUtil.generateAccessToken(
                testUserId,
                List.of("EV_SAHIBI"),
                List.of(),
                List.of(testPropertyId)
        );

        mockMvc.perform(get("/api/v1/properties/mine")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}

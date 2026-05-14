package com.ekokoy.portal.user;

import com.ekokoy.portal.config.JwtUtil;
import com.ekokoy.portal.user.repository.RoleRepository;
import com.ekokoy.portal.user.repository.UserRepository;
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

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class RoleControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;

    private String superAdminToken;
    private UUID superAdminUserId;
    private UUID evSahibiRoleId;

    @BeforeEach
    void setUp() {
        var superAdminUser = userRepository.findByEmailAndIsDeletedFalse("admin@ekokoy.com");
        superAdminUserId = superAdminUser.map(u -> u.getId()).orElse(UUID.randomUUID());

        evSahibiRoleId = roleRepository.findByCode("EV_SAHIBI")
                .map(r -> r.getId())
                .orElse(UUID.randomUUID());

        List<String> roles = List.of("SUPER_ADMIN");
        List<String> permissions = List.of("USER_ASSIGN_ROLE", "SYSTEM_ADMIN");
        superAdminToken = jwtUtil.generateAccessToken(superAdminUserId, roles, permissions, List.of());
    }

    @Test
    void should_return_roles_list_when_super_admin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(7));
    }

    @Test
    void should_return_401_when_no_token() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_role_permissions_when_valid_id() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles/{id}/permissions", evSahibiRoleId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("EV_SAHIBI"))
                .andExpect(jsonPath("$.data.permissions").isArray());
    }

    @Test
    void should_return_my_permissions_when_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/me/permissions")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void should_assign_role_and_block_last_role_removal() throws Exception {
        // Test kullanıcısı oluştur
        var testUser = new com.ekokoy.portal.user.entity.User();
        testUser.setFirstName("Test");
        testUser.setLastName("Kullanıcı");
        testUser.setEmail("test-role-" + UUID.randomUUID() + "@ekokoy.com");
        testUser.setStatus(com.ekokoy.portal.user.entity.UserStatus.active);
        testUser = userRepository.save(testUser);
        UUID testUserId = testUser.getId();

        UUID ziyaretciRoleId = roleRepository.findByCode("ZIYARETCI")
                .map(r -> r.getId())
                .orElseThrow();

        // Rol ata
        mockMvc.perform(post("/api/v1/admin/users/{id}/roles", testUserId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":\"" + ziyaretciRoleId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.roleCode").value("ZIYARETCI"));

        // Kullanıcının rollerini getir
        mockMvc.perform(get("/api/v1/admin/users/{id}/roles", testUserId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // Son rolü kaldırmaya çalış — 422 beklenir
        mockMvc.perform(delete("/api/v1/admin/users/{id}/roles/{roleId}", testUserId, ziyaretciRoleId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isUnprocessableEntity());
    }
}

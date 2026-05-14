package com.ekokoy.portal.user;

import com.ekokoy.portal.config.JwtUtil;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserStatus;
import com.ekokoy.portal.user.repository.RefreshTokenRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User activeUser;
    private String rawPassword = "TestPass1!";
    private String accessToken;

    @BeforeEach
    void setUp() {
        // Seed SUPER_ADMIN zaten var; ayrıca test kullanıcısı oluştur
        activeUser = userRepository.findByEmailAndIsDeletedFalse("auth-test@ekokoy.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setFirstName("Auth");
                    u.setLastName("Test");
                    u.setEmail("auth-test@ekokoy.com");
                    u.setPasswordHash(passwordEncoder.encode(rawPassword));
                    u.setStatus(UserStatus.active);
                    return userRepository.save(u);
                });

        accessToken = jwtUtil.generateAccessToken(
                activeUser.getId(),
                activeUser.getEmail(),
                List.of("EV_SAHIBI"),
                List.of(),
                List.of()
        );
    }

    @Test
    void should_return_tokens_when_login_with_valid_credentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"auth-test@ekokoy.com","password":"TestPass1!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void should_return_401_when_login_with_wrong_password() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"auth-test@ekokoy.com","password":"WrongPass1!"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_401_when_login_with_nonexistent_email() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@ekokoy.com","password":"SomePass1!"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_rotate_refresh_token_on_refresh() throws Exception {
        // 1. Login yaparak refresh token al
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"auth-test@ekokoy.com","password":"TestPass1!"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = extractJsonField(loginResponse, "refreshToken");

        // 2. Refresh ile yeni token çifti al
        String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String newRefreshToken = extractJsonField(refreshResponse, "refreshToken");

        // 3. Eski token tekrar kullanılamaz (revoke tespiti → tüm oturumlar kapatılır)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_logout_and_invalidate_refresh_token() throws Exception {
        // Login
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"auth-test@ekokoy.com","password":"TestPass1!"}
                                """))
                .andReturn().getResponse().getContentAsString();

        String refreshToken = extractJsonField(loginResponse, "refreshToken");

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk());

        // Artık refresh çalışmamalı
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_me_when_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("auth-test@ekokoy.com"))
                .andExpect(jsonPath("$.data.firstName").value("Auth"));
    }

    @Test
    void should_return_401_when_me_without_token() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_sessions_when_authenticated() throws Exception {
        // Login yaparak oturum oluştur
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"auth-test@ekokoy.com","password":"TestPass1!"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/sessions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void should_accept_forgot_password_for_unknown_email() throws Exception {
        // User enumeration önlemi: bilinmeyen e-posta için de 200 dönmeli
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@ekokoy.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_return_400_when_reset_password_with_invalid_token() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"nonexistent-token","newPassword":"NewPass1!"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // Basit JSON alan çıkarma yardımcısı
    private String extractJsonField(String json, String field) {
        String search = "\"" + field + "\":\"";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}

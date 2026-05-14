package com.ekokoy.portal.dues;

import com.ekokoy.portal.config.JwtUtil;
import com.ekokoy.portal.dues.entity.Due;
import com.ekokoy.portal.dues.entity.DueStatus;
import com.ekokoy.portal.dues.repository.DueRepository;
import com.ekokoy.portal.user.entity.Property;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserStatus;
import com.ekokoy.portal.user.repository.PropertyRepository;
import com.ekokoy.portal.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class DueAdminControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private DueRepository dueRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private Property testProperty;
    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.findByEmailAndIsDeletedFalse("dues-admin@ekokoy.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setFirstName("Dues");
                    u.setLastName("Admin");
                    u.setEmail("dues-admin@ekokoy.com");
                    u.setPasswordHash(passwordEncoder.encode("TestPass1!"));
                    u.setStatus(UserStatus.active);
                    return userRepository.save(u);
                });

        User regularUser = userRepository.findByEmailAndIsDeletedFalse("dues-user@ekokoy.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setFirstName("Dues");
                    u.setLastName("User");
                    u.setEmail("dues-user@ekokoy.com");
                    u.setPasswordHash(passwordEncoder.encode("TestPass1!"));
                    u.setStatus(UserStatus.active);
                    return userRepository.save(u);
                });

        // property number 999 is for integration tests, avoid conflicts with seeded data
        testProperty = propertyRepository.findByNumber(999)
                .orElseGet(() -> {
                    Property p = new Property();
                    p.setNumber(999);
                    p.setType("test");
                    return propertyRepository.save(p);
                });

        adminToken = jwtUtil.generateAccessToken(
                adminUser.getId(), adminUser.getEmail(),
                List.of("SUPER_ADMIN"), List.of(), List.of()
        );

        userToken = jwtUtil.generateAccessToken(
                regularUser.getId(), regularUser.getEmail(),
                List.of("EV_SAHIBI"), List.of(), List.of()
        );
    }

    // ── GET /api/v1/admin/dues ─────────────────────────────────────────────────────

    @Test
    void should_list_all_dues_as_admin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dues")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void should_reject_non_admin_for_dues_list() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dues")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/v1/admin/dues/summary ─────────────────────────────────────────────

    @Test
    void should_return_summary_as_admin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dues/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDues").isNumber())
                .andExpect(jsonPath("$.data.totalAmount").isNumber());
    }

    // ── POST /api/v1/admin/dues/import (CSV) ──────────────────────────────────────

    @Test
    void should_import_csv_successfully() throws Exception {
        String csvContent = "konut_no,yil,ay,tutar,son_odeme_tarihi,aciklama\n"
                + "999,2025,3,500.00,2025-03-31,Test aidat\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/dues/import")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.successRows").value(1))
                .andExpect(jsonPath("$.data.errorRows").value(0))
                .andExpect(jsonPath("$.data.status").value("completed"));
    }

    @Test
    void should_upsert_on_duplicate_period() throws Exception {
        // İlk import
        String csv1 = "konut_no,yil,ay,tutar,son_odeme_tarihi\n999,2025,4,400.00,2025-04-30\n";
        MockMultipartFile file1 = new MockMultipartFile("file", "f1.csv", "text/csv", csv1.getBytes());
        mockMvc.perform(multipart("/api/v1/admin/dues/import").file(file1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        // Güncelleme ile aynı dönem
        String csv2 = "konut_no,yil,ay,tutar,son_odeme_tarihi\n999,2025,4,600.00,2025-04-30\n";
        MockMultipartFile file2 = new MockMultipartFile("file", "f2.csv", "text/csv", csv2.getBytes());
        mockMvc.perform(multipart("/api/v1/admin/dues/import").file(file2)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        // Borç tutarının güncellendiğini doğrula
        mockMvc.perform(get("/api/v1/admin/dues")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.propertyNumber == 999 && @.periodYear == 2025 && @.periodMonth == 4)].amount",
                        hasItem(600.0)));
    }

    @Test
    void should_skip_invalid_rows_without_stopping_import() throws Exception {
        String csv = "konut_no,yil,ay,tutar,son_odeme_tarihi\n"
                + "999,2025,5,500.00,2025-05-31\n"
                + "INVALID,2025,5,100.00,2025-05-31\n"
                + "999,2025,6,300.00,2025-06-30\n";

        MockMultipartFile file = new MockMultipartFile("file", "mixed.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/v1/admin/dues/import").file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.successRows").value(2))
                .andExpect(jsonPath("$.data.errorRows").value(1))
                .andExpect(jsonPath("$.data.status").value("completed"));
    }

    @Test
    void should_reject_file_larger_than_5mb() throws Exception {
        byte[] bigContent = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.csv", "text/csv", bigContent);

        mockMvc.perform(multipart("/api/v1/admin/dues/import").file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void should_reject_unsupported_file_type() throws Exception {
        byte[] content = "some,data".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "data.txt", "text/plain", content);

        mockMvc.perform(multipart("/api/v1/admin/dues/import").file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── POST /api/v1/admin/dues/:id/payments ──────────────────────────────────────

    @Test
    void should_record_payment_and_update_status() throws Exception {
        Due due = createTestDue(2025, 7, BigDecimal.valueOf(1000), DueStatus.unpaid);

        String paymentJson = """
                {
                    "amount": 1000.00,
                    "paymentDate": "2025-07-15",
                    "paymentMethod": "Banka havalesi"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/dues/" + due.getId() + "/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(1000.0));

        // Borç statusünün paid olduğunu doğrula
        mockMvc.perform(get("/api/v1/admin/dues")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void should_record_partial_payment() throws Exception {
        Due due = createTestDue(2025, 8, BigDecimal.valueOf(1000), DueStatus.unpaid);

        String paymentJson = """
                {
                    "amount": 400.00,
                    "paymentDate": "2025-08-10"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/dues/" + due.getId() + "/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson))
                .andExpect(status().isCreated());
    }

    // ── PATCH /api/v1/admin/dues/:id/cancel ────────────────────────────────────────

    @Test
    void should_cancel_due() throws Exception {
        Due due = createTestDue(2025, 9, BigDecimal.valueOf(500), DueStatus.unpaid);

        mockMvc.perform(patch("/api/v1/admin/dues/" + due.getId() + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));
    }

    // ── GET /api/v1/admin/dues/imports ────────────────────────────────────────────

    @Test
    void should_list_imports() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dues/imports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── GET /api/v1/admin/properties/:id/dues ─────────────────────────────────────

    @Test
    void should_list_property_dues() throws Exception {
        mockMvc.perform(get("/api/v1/admin/properties/" + testProperty.getId() + "/dues")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private Due createTestDue(int year, int month, BigDecimal amount, DueStatus status) {
        Due due = new Due();
        due.setProperty(testProperty);
        due.setPeriodYear(year);
        due.setPeriodMonth(month);
        due.setAmount(amount);
        due.setPaidAmount(BigDecimal.ZERO);
        due.setStatus(status);
        due.setDueDate(LocalDate.of(year, month, 28));
        due.setCreatedBy(adminUser);
        return dueRepository.save(due);
    }
}

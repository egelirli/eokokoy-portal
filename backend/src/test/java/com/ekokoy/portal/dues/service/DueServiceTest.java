package com.ekokoy.portal.dues.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.dues.dto.DuePaymentResponse;
import com.ekokoy.portal.dues.dto.DueResponse;
import com.ekokoy.portal.dues.dto.DueSummaryResponse;
import com.ekokoy.portal.dues.dto.RecordPaymentRequest;
import com.ekokoy.portal.dues.entity.Due;
import com.ekokoy.portal.dues.entity.DuePayment;
import com.ekokoy.portal.dues.entity.DueStatus;
import com.ekokoy.portal.dues.repository.DuePaymentRepository;
import com.ekokoy.portal.dues.repository.DueRepository;
import com.ekokoy.portal.user.entity.Property;
import com.ekokoy.portal.user.entity.PropertyUser;
import com.ekokoy.portal.user.entity.RelationType;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserStatus;
import com.ekokoy.portal.user.repository.PropertyUserRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DueServiceTest {

    @Mock private DueRepository dueRepository;
    @Mock private DuePaymentRepository paymentRepository;
    @Mock private PropertyUserRepository propertyUserRepository;
    @Mock private UserRepository userRepository;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private DueService dueService;

    private final UUID userId = UUID.randomUUID();
    private final UUID dueId = UUID.randomUUID();
    private final UUID propertyId = UUID.randomUUID();

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

    // ── getMyDues ──────────────────────────────────────────────────────────────────

    @Test
    void should_return_empty_when_no_properties() {
        when(propertyUserRepository.findActiveByUserId(userId)).thenReturn(List.of());

        List<DueResponse> result = dueService.getMyDues();

        assertThat(result).isEmpty();
    }

    @Test
    void should_return_dues_for_user_properties() {
        PropertyUser pu = makePropertyUser();
        Due due = makeDue(BigDecimal.valueOf(500), BigDecimal.ZERO);
        when(propertyUserRepository.findActiveByUserId(userId)).thenReturn(List.of(pu));
        when(dueRepository.findByPropertyIdIn(List.of(propertyId))).thenReturn(List.of(due));

        List<DueResponse> result = dueService.getMyDues();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(DueStatus.unpaid);
    }

    // ── getMyDuesSummary ───────────────────────────────────────────────────────────

    @Test
    void should_return_correct_summary() {
        PropertyUser pu = makePropertyUser();
        Due unpaid = makeDue(BigDecimal.valueOf(500), BigDecimal.ZERO);
        Due partial = makeDue(BigDecimal.valueOf(1000), BigDecimal.valueOf(400));
        partial.recalculateStatus();
        when(propertyUserRepository.findActiveByUserId(userId)).thenReturn(List.of(pu));
        when(dueRepository.findByPropertyIdIn(any())).thenReturn(List.of(unpaid, partial));

        DueSummaryResponse summary = dueService.getMyDuesSummary();

        assertThat(summary.totalDues()).isEqualTo(2);
        assertThat(summary.unpaidDues()).isEqualTo(1);
        assertThat(summary.partiallyPaidDues()).isEqualTo(1);
        assertThat(summary.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(summary.totalPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(400));
    }

    // ── recordPayment ──────────────────────────────────────────────────────────────

    @Test
    void should_record_payment_and_update_status_to_paid() {
        Due due = makeDue(BigDecimal.valueOf(500), BigDecimal.ZERO);
        when(dueRepository.findById(dueId)).thenReturn(Optional.of(due));
        when(userRepository.getReferenceById(userId)).thenReturn(makeUser());

        DuePayment payment = new DuePayment();
        payment.setDue(due);
        payment.setAmount(BigDecimal.valueOf(500));
        payment.setPaymentDate(LocalDate.now());
        payment.setRecordedBy(makeUser());
        when(paymentRepository.save(any())).thenReturn(payment);
        when(paymentRepository.sumAmountByDueId(dueId)).thenReturn(BigDecimal.valueOf(500));
        when(dueRepository.save(any())).thenReturn(due);

        RecordPaymentRequest req = new RecordPaymentRequest(
                BigDecimal.valueOf(500), LocalDate.now(), "Nakit", null, null
        );

        DuePaymentResponse result = dueService.recordPayment(dueId, req);

        assertThat(result).isNotNull();
        assertThat(due.getStatus()).isEqualTo(DueStatus.paid);
    }

    @Test
    void should_record_partial_payment_and_update_status() {
        Due due = makeDue(BigDecimal.valueOf(1000), BigDecimal.ZERO);
        when(dueRepository.findById(dueId)).thenReturn(Optional.of(due));
        when(userRepository.getReferenceById(userId)).thenReturn(makeUser());

        DuePayment payment = new DuePayment();
        payment.setDue(due);
        payment.setAmount(BigDecimal.valueOf(300));
        payment.setPaymentDate(LocalDate.now());
        payment.setRecordedBy(makeUser());
        when(paymentRepository.save(any())).thenReturn(payment);
        when(paymentRepository.sumAmountByDueId(dueId)).thenReturn(BigDecimal.valueOf(300));
        when(dueRepository.save(any())).thenReturn(due);

        RecordPaymentRequest req = new RecordPaymentRequest(
                BigDecimal.valueOf(300), LocalDate.now(), null, null, null
        );

        dueService.recordPayment(dueId, req);

        assertThat(due.getStatus()).isEqualTo(DueStatus.partially_paid);
    }

    @Test
    void should_throw_when_recording_payment_to_cancelled_due() {
        Due due = makeDue(BigDecimal.valueOf(500), BigDecimal.ZERO);
        due.setStatus(DueStatus.cancelled);
        when(dueRepository.findById(dueId)).thenReturn(Optional.of(due));

        RecordPaymentRequest req = new RecordPaymentRequest(
                BigDecimal.valueOf(100), LocalDate.now(), null, null, null
        );

        assertThatThrownBy(() -> dueService.recordPayment(dueId, req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("DUE_CANCELLED");
    }

    // ── deletePayment ──────────────────────────────────────────────────────────────

    @Test
    void should_delete_payment_and_recalculate_status() {
        Due due = makeDue(BigDecimal.valueOf(500), BigDecimal.valueOf(500));
        due.setStatus(DueStatus.paid);

        DuePayment payment = new DuePayment();
        payment.setDue(due);
        payment.setAmount(BigDecimal.valueOf(500));
        payment.setPaymentDate(LocalDate.now());
        payment.setRecordedBy(makeUser());

        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.sumAmountByDueId(any())).thenReturn(BigDecimal.ZERO);
        when(dueRepository.save(any())).thenReturn(due);

        dueService.deletePayment(paymentId);

        verify(paymentRepository).delete(payment);
        assertThat(due.getStatus()).isEqualTo(DueStatus.unpaid);
    }

    @Test
    void should_throw_when_payment_not_found() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dueService.deletePayment(paymentId))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("PAYMENT_NOT_FOUND");
    }

    // ── cancelDue ─────────────────────────────────────────────────────────────────

    @Test
    void should_cancel_due() {
        Due due = makeDue(BigDecimal.valueOf(500), BigDecimal.ZERO);
        when(dueRepository.findById(dueId)).thenReturn(Optional.of(due));
        when(dueRepository.save(any())).thenReturn(due);

        DueResponse result = dueService.cancelDue(dueId);

        assertThat(result).isNotNull();
        assertThat(due.getStatus()).isEqualTo(DueStatus.cancelled);
    }

    @Test
    void should_throw_when_cancelling_already_cancelled_due() {
        Due due = makeDue(BigDecimal.valueOf(500), BigDecimal.ZERO);
        due.setStatus(DueStatus.cancelled);
        when(dueRepository.findById(dueId)).thenReturn(Optional.of(due));

        assertThatThrownBy(() -> dueService.cancelDue(dueId))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("ALREADY_CANCELLED");
    }

    // ── Due.recalculateStatus ──────────────────────────────────────────────────────

    @Test
    void should_set_unpaid_when_paid_amount_is_zero() {
        Due due = makeDue(BigDecimal.valueOf(500), BigDecimal.ZERO);
        due.recalculateStatus();
        assertThat(due.getStatus()).isEqualTo(DueStatus.unpaid);
    }

    @Test
    void should_set_partially_paid_when_partial_payment() {
        Due due = makeDue(BigDecimal.valueOf(500), BigDecimal.valueOf(200));
        due.recalculateStatus();
        assertThat(due.getStatus()).isEqualTo(DueStatus.partially_paid);
    }

    @Test
    void should_set_paid_when_fully_paid() {
        Due due = makeDue(BigDecimal.valueOf(500), BigDecimal.valueOf(500));
        due.recalculateStatus();
        assertThat(due.getStatus()).isEqualTo(DueStatus.paid);
        assertThat(due.getPaidAt()).isNotNull();
    }

    @Test
    void should_not_change_status_when_cancelled() {
        Due due = makeDue(BigDecimal.valueOf(500), BigDecimal.valueOf(500));
        due.setStatus(DueStatus.cancelled);
        due.recalculateStatus();
        assertThat(due.getStatus()).isEqualTo(DueStatus.cancelled);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private Due makeDue(BigDecimal amount, BigDecimal paidAmount) {
        Property property = new Property();
        property.setId(propertyId);
        property.setNumber(1);

        Due due = new Due();
        due.setProperty(property);
        due.setPeriodYear(2025);
        due.setPeriodMonth(1);
        due.setAmount(amount);
        due.setPaidAmount(paidAmount);
        due.setDueDate(LocalDate.of(2025, 1, 31));
        due.setCreatedBy(makeUser());
        return due;
    }

    private PropertyUser makePropertyUser() {
        Property property = new Property();
        property.setId(propertyId);
        property.setNumber(1);

        PropertyUser pu = new PropertyUser();
        pu.setProperty(property);
        pu.setUser(makeUser());
        pu.setRelationType(RelationType.ev_sahibi);
        pu.setStartDate(LocalDate.now().minusYears(1));
        return pu;
    }

    private User makeUser() {
        User u = new User();
        u.setId(userId);
        u.setFirstName("Test");
        u.setLastName("Kullanıcı");
        u.setEmail("test@ekokoy.com");
        u.setStatus(UserStatus.active);
        return u;
    }
}

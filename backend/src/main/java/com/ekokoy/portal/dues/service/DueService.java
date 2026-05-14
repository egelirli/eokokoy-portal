package com.ekokoy.portal.dues.service;

import com.ekokoy.portal.dues.dto.*;
import com.ekokoy.portal.dues.entity.Due;
import com.ekokoy.portal.dues.entity.DuePayment;
import com.ekokoy.portal.dues.entity.DueStatus;
import com.ekokoy.portal.dues.repository.DuePaymentRepository;
import com.ekokoy.portal.dues.repository.DueRepository;
import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.repository.PropertyUserRepository;
import com.ekokoy.portal.user.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class DueService {

    private final DueRepository dueRepository;
    private final DuePaymentRepository paymentRepository;
    private final PropertyUserRepository propertyUserRepository;
    private final UserRepository userRepository;

    public DueService(DueRepository dueRepository,
                      DuePaymentRepository paymentRepository,
                      PropertyUserRepository propertyUserRepository,
                      UserRepository userRepository) {
        this.dueRepository = dueRepository;
        this.paymentRepository = paymentRepository;
        this.propertyUserRepository = propertyUserRepository;
        this.userRepository = userRepository;
    }

    /** Giriş yapan ev sahibinin konutlarına ait borçları listeler. */
    @Transactional(readOnly = true)
    public List<DueResponse> getMyDues() {
        List<UUID> propertyIds = getMyPropertyIds();
        if (propertyIds.isEmpty()) return List.of();
        return dueRepository.findByPropertyIdIn(propertyIds)
                .stream().map(DueResponse::from).toList();
    }

    /** Giriş yapan ev sahibinin borç özetini döner. */
    @Transactional(readOnly = true)
    public DueSummaryResponse getMyDuesSummary() {
        List<UUID> propertyIds = getMyPropertyIds();
        if (propertyIds.isEmpty()) return emptySummary();
        List<Due> dues = dueRepository.findByPropertyIdIn(propertyIds);
        return buildSummary(dues);
    }

    /** Tüm borçları listeler (admin). */
    @Transactional(readOnly = true)
    public List<DueResponse> getAllDues() {
        return dueRepository.findAllWithProperty()
                .stream().map(DueResponse::from).toList();
    }

    /** Tüm borçların özetini döner (admin). */
    @Transactional(readOnly = true)
    public DueSummaryResponse getAllDuesSummary() {
        return buildSummary(dueRepository.findAllWithProperty());
    }

    /** Belirli bir konutun borçlarını listeler (admin). */
    @Transactional(readOnly = true)
    public List<DueResponse> getPropertyDues(UUID propertyId) {
        return dueRepository.findByPropertyId(propertyId)
                .stream().map(DueResponse::from).toList();
    }

    /** Borca ödeme kaydeder ve status'ü otomatik günceller. */
    @Transactional
    public DuePaymentResponse recordPayment(UUID dueId, RecordPaymentRequest req) {
        Due due = requireDue(dueId);
        if (due.getStatus() == DueStatus.cancelled) {
            throw new EkokoyException("DUE_CANCELLED", "İptal edilmiş borca ödeme eklenemez.", 422);
        }

        User recorder = userRepository.getReferenceById(currentUserId());

        DuePayment payment = new DuePayment();
        payment.setDue(due);
        payment.setAmount(req.amount());
        payment.setPaymentDate(req.paymentDate());
        payment.setPaymentMethod(req.paymentMethod());
        payment.setReferenceNo(req.referenceNo());
        payment.setNotes(req.notes());
        payment.setRecordedBy(recorder);
        paymentRepository.save(payment);

        BigDecimal totalPaid = paymentRepository.sumAmountByDueId(dueId);
        due.setPaidAmount(totalPaid);
        due.recalculateStatus();
        dueRepository.save(due);

        return DuePaymentResponse.from(payment);
    }

    /** Bir borcun ödemelerini listeler. */
    @Transactional(readOnly = true)
    public List<DuePaymentResponse> getPayments(UUID dueId) {
        requireDue(dueId);
        return paymentRepository.findByDueId(dueId)
                .stream().map(DuePaymentResponse::from).toList();
    }

    /** Ödeme kaydını siler ve borcun status'ünü yeniden hesaplar (SUPER_ADMIN). */
    @Transactional
    public void deletePayment(UUID paymentId) {
        DuePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EkokoyException("PAYMENT_NOT_FOUND", "Ödeme kaydı bulunamadı.", 404));

        Due due = payment.getDue();
        paymentRepository.delete(payment);

        BigDecimal totalPaid = paymentRepository.sumAmountByDueId(due.getId());
        due.setPaidAmount(totalPaid);
        due.recalculateStatus();
        dueRepository.save(due);
    }

    /** Borcu iptal eder (SUPER_ADMIN). */
    @Transactional
    public DueResponse cancelDue(UUID dueId) {
        Due due = requireDue(dueId);
        if (due.getStatus() == DueStatus.cancelled) {
            throw new EkokoyException("ALREADY_CANCELLED", "Borç zaten iptal edilmiş.", 422);
        }
        due.setStatus(DueStatus.cancelled);
        dueRepository.save(due);
        return DueResponse.from(due);
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────────

    private Due requireDue(UUID id) {
        return dueRepository.findById(id)
                .orElseThrow(() -> new EkokoyException("DUE_NOT_FOUND", "Borç bulunamadı: " + id, 404));
    }

    private List<UUID> getMyPropertyIds() {
        UUID userId = currentUserId();
        return propertyUserRepository.findActiveByUserId(userId)
                .stream().map(pu -> pu.getProperty().getId()).toList();
    }

    private UUID currentUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private DueSummaryResponse buildSummary(List<Due> dues) {
        long total = dues.size();
        long unpaid = dues.stream().filter(d -> d.getStatus() == DueStatus.unpaid).count();
        long partial = dues.stream().filter(d -> d.getStatus() == DueStatus.partially_paid).count();
        long paid = dues.stream().filter(d -> d.getStatus() == DueStatus.paid).count();
        long cancelled = dues.stream().filter(d -> d.getStatus() == DueStatus.cancelled).count();

        BigDecimal totalAmount = dues.stream().map(Due::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = dues.stream().map(Due::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = totalAmount.subtract(totalPaid);

        return new DueSummaryResponse(total, unpaid, partial, paid, cancelled, totalAmount, totalPaid, totalRemaining);
    }

    private DueSummaryResponse emptySummary() {
        return new DueSummaryResponse(0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}

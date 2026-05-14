package com.ekokoy.portal.dues.entity;

import com.ekokoy.portal.user.entity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "due_payments")
@EntityListeners(AuditingEntityListener.class)
public class DuePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "due_id", nullable = false)
    private Due due;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public Due getDue() { return due; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getReferenceNo() { return referenceNo; }
    public String getNotes() { return notes; }
    public User getRecordedBy() { return recordedBy; }
    public Instant getCreatedAt() { return createdAt; }

    public void setDue(Due due) { this.due = due; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setRecordedBy(User recordedBy) { this.recordedBy = recordedBy; }
}

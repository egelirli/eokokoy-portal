package com.ekokoy.portal.dues.entity;

import com.ekokoy.portal.user.entity.Property;
import com.ekokoy.portal.user.entity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "dues")
@EntityListeners(AuditingEntityListener.class)
public class Due {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month")
    private Integer periodMonth;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DueStatus status = DueStatus.unpaid;

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDate paidAt;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_batch_id")
    private DueImport importBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public Property getProperty() { return property; }
    public User getUser() { return user; }
    public int getPeriodYear() { return periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public BigDecimal getAmount() { return amount; }
    public DueStatus getStatus() { return status; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getPaidAt() { return paidAt; }
    public String getDescription() { return description; }
    public DueImport getImportBatch() { return importBatch; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setProperty(Property property) { this.property = property; }
    public void setUser(User user) { this.user = user; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setStatus(DueStatus status) { this.status = status; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setPaidAt(LocalDate paidAt) { this.paidAt = paidAt; }
    public void setDescription(String description) { this.description = description; }
    public void setImportBatch(DueImport importBatch) { this.importBatch = importBatch; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    /** paid_amount değerine göre status'ü yeniden hesaplar. */
    public void recalculateStatus() {
        if (this.status == DueStatus.cancelled) return;
        int cmp = this.paidAmount.compareTo(BigDecimal.ZERO);
        if (cmp == 0) {
            this.status = DueStatus.unpaid;
        } else if (this.paidAmount.compareTo(this.amount) >= 0) {
            this.status = DueStatus.paid;
            if (this.paidAt == null) this.paidAt = LocalDate.now();
        } else {
            this.status = DueStatus.partially_paid;
        }
    }
}

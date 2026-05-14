package com.ekokoy.portal.dues.entity;

import com.ekokoy.portal.user.entity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "due_imports")
@EntityListeners(AuditingEntityListener.class)
public class DueImport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by", nullable = false)
    private User importedBy;

    @Column(name = "total_rows", nullable = false)
    private int totalRows = 0;

    @Column(name = "success_rows", nullable = false)
    private int successRows = 0;

    @Column(name = "error_rows", nullable = false)
    private int errorRows = 0;

    @Column(name = "error_details", columnDefinition = "JSONB")
    private String errorDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportStatus status = ImportStatus.processing;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public UUID getId() { return id; }
    public String getFileName() { return fileName; }
    public User getImportedBy() { return importedBy; }
    public int getTotalRows() { return totalRows; }
    public int getSuccessRows() { return successRows; }
    public int getErrorRows() { return errorRows; }
    public String getErrorDetails() { return errorDetails; }
    public ImportStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setImportedBy(User importedBy) { this.importedBy = importedBy; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public void setSuccessRows(int successRows) { this.successRows = successRows; }
    public void setErrorRows(int errorRows) { this.errorRows = errorRows; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
    public void setStatus(ImportStatus status) { this.status = status; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}

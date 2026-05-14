package com.ekokoy.portal.storage.entity;

import com.ekokoy.portal.user.entity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "files")
@EntityListeners(AuditingEntityListener.class)
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bucket", nullable = false, length = 50)
    private String bucket;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private FileType fileType;

    @Column(name = "thumbnail_key", length = 512)
    private String thumbnailKey;

    @Column(name = "compressed_key", length = 512)
    private String compressedKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "is_confirmed", nullable = false)
    private boolean isConfirmed = false;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public String getBucket() { return bucket; }
    public String getObjectKey() { return objectKey; }
    public String getOriginalName() { return originalName; }
    public String getMimeType() { return mimeType; }
    public Long getFileSize() { return fileSize; }
    public FileType getFileType() { return fileType; }
    public String getThumbnailKey() { return thumbnailKey; }
    public String getCompressedKey() { return compressedKey; }
    public User getUploadedBy() { return uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
    public boolean isConfirmed() { return isConfirmed; }
    public boolean isDeleted() { return isDeleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public User getDeletedBy() { return deletedBy; }
    public String getChecksum() { return checksum; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(UUID id) { this.id = id; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setFileType(FileType fileType) { this.fileType = fileType; }
    public void setThumbnailKey(String thumbnailKey) { this.thumbnailKey = thumbnailKey; }
    public void setCompressedKey(String compressedKey) { this.compressedKey = compressedKey; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    public void setConfirmed(boolean confirmed) { isConfirmed = confirmed; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public void setDeletedBy(User deletedBy) { this.deletedBy = deletedBy; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
}

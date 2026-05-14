package com.ekokoy.portal.announcement.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcement_attachments")
@EntityListeners(AuditingEntityListener.class)
public class AnnouncementAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Column(name = "file_url", nullable = false, length = 512)
    private String fileUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private AttachmentFileType fileType;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public Announcement getAnnouncement() { return announcement; }
    public String getFileUrl() { return fileUrl; }
    public String getFileName() { return fileName; }
    public AttachmentFileType getFileType() { return fileType; }
    public Integer getFileSize() { return fileSize; }
    public int getDisplayOrder() { return displayOrder; }
    public Instant getCreatedAt() { return createdAt; }

    public void setAnnouncement(Announcement announcement) { this.announcement = announcement; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setFileType(AttachmentFileType fileType) { this.fileType = fileType; }
    public void setFileSize(Integer fileSize) { this.fileSize = fileSize; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}

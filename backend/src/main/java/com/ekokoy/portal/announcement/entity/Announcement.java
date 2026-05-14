package com.ekokoy.portal.announcement.entity;

import com.ekokoy.portal.user.entity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "announcements")
@EntityListeners(AuditingEntityListener.class)
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementPriority priority = AnnouncementPriority.normal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementStatus status = AnnouncementStatus.draft;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private AnnouncementTargetType targetType = AnnouncementTargetType.all;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<AnnouncementAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AnnouncementTarget> targets = new ArrayList<>();

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public AnnouncementPriority getPriority() { return priority; }
    public AnnouncementStatus getStatus() { return status; }
    public boolean isPublic() { return isPublic; }
    public AnnouncementTargetType getTargetType() { return targetType; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<AnnouncementAttachment> getAttachments() { return attachments; }
    public List<AnnouncementTarget> getTargets() { return targets; }

    public void setTitle(String title) { this.title = title; }
    public void setBody(String body) { this.body = body; }
    public void setPriority(AnnouncementPriority priority) { this.priority = priority; }
    public void setStatus(AnnouncementStatus status) { this.status = status; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public void setTargetType(AnnouncementTargetType targetType) { this.targetType = targetType; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}

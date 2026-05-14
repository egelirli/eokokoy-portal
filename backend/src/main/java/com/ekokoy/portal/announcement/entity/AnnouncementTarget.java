package com.ekokoy.portal.announcement.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "announcement_targets")
public class AnnouncementTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_entity_type", nullable = false, length = 20)
    private AnnouncementTargetEntityType targetEntityType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    public UUID getId() { return id; }
    public Announcement getAnnouncement() { return announcement; }
    public AnnouncementTargetEntityType getTargetEntityType() { return targetEntityType; }
    public UUID getTargetId() { return targetId; }

    public void setAnnouncement(Announcement announcement) { this.announcement = announcement; }
    public void setTargetEntityType(AnnouncementTargetEntityType targetEntityType) { this.targetEntityType = targetEntityType; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
}

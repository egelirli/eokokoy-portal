package com.ekokoy.portal.announcement.entity;

import com.ekokoy.portal.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "announcement_reads")
@IdClass(AnnouncementReadId.class)
public class AnnouncementRead {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at", nullable = false)
    private Instant readAt = Instant.now();

    public Announcement getAnnouncement() { return announcement; }
    public User getUser() { return user; }
    public Instant getReadAt() { return readAt; }

    public void setAnnouncement(Announcement announcement) { this.announcement = announcement; }
    public void setUser(User user) { this.user = user; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}

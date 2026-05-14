package com.ekokoy.portal.announcement.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class AnnouncementReadId implements Serializable {

    private UUID announcement;
    private UUID user;

    public AnnouncementReadId() {}

    public AnnouncementReadId(UUID announcement, UUID user) {
        this.announcement = announcement;
        this.user = user;
    }

    public UUID getAnnouncement() { return announcement; }
    public UUID getUser() { return user; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnouncementReadId that)) return false;
        return Objects.equals(announcement, that.announcement) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(announcement, user);
    }
}

package com.ekokoy.portal.user.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserRoleId implements Serializable {

    private UUID user;
    private UUID role;

    public UserRoleId() {}

    public UserRoleId(UUID user, UUID role) {
        this.user = user;
        this.role = role;
    }

    public UUID getUser() { return user; }
    public UUID getRole() { return role; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRoleId that)) return false;
        return Objects.equals(user, that.user) && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, role);
    }
}

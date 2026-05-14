package com.ekokoy.portal.user.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleId.class)
public class UserRole {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    public UserRole() {}

    public UserRole(User user, Role role, User assignedBy) {
        this.user = user;
        this.role = role;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    public User getUser() { return user; }
    public Role getRole() { return role; }
    public Instant getAssignedAt() { return assignedAt; }
    public User getAssignedBy() { return assignedBy; }

    public void setUser(User user) { this.user = user; }
    public void setRole(Role role) { this.role = role; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }
}

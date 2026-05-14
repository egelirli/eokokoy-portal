package com.ekokoy.portal.poll.entity;

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
@Table(name = "polls")
@EntityListeners(AuditingEntityListener.class)
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollStatus status = PollStatus.draft;

    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous = false;

    @Column(name = "eligible_roles", columnDefinition = "text[]")
    private String[] eligibleRoles;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "poll", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    private List<PollQuestion> questions = new ArrayList<>();

    public UUID getId() { return id; }
    public PollType getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public PollStatus getStatus() { return status; }
    public boolean isAnonymous() { return isAnonymous; }
    public String[] getEligibleRoles() { return eligibleRoles; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public User getCreatedBy() { return createdBy; }
    public User getClosedBy() { return closedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<PollQuestion> getQuestions() { return questions; }

    public void setId(UUID id) { this.id = id; }
    public void setType(PollType type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(PollStatus status) { this.status = status; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }
    public void setEligibleRoles(String[] eligibleRoles) { this.eligibleRoles = eligibleRoles; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public void setClosedBy(User closedBy) { this.closedBy = closedBy; }
    public void setQuestions(List<PollQuestion> questions) { this.questions = questions; }
}

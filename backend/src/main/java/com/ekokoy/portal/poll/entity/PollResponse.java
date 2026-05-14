package com.ekokoy.portal.poll.entity;

import com.ekokoy.portal.user.entity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "poll_responses")
@EntityListeners(AuditingEntityListener.class)
public class PollResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private PollQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private PollOption option;

    @Column(name = "text_answer", columnDefinition = "TEXT")
    private String textAnswer;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public Poll getPoll() { return poll; }
    public PollQuestion getQuestion() { return question; }
    public User getUser() { return user; }
    public PollOption getOption() { return option; }
    public String getTextAnswer() { return textAnswer; }
    public Instant getCreatedAt() { return createdAt; }

    public void setPoll(Poll poll) { this.poll = poll; }
    public void setQuestion(PollQuestion question) { this.question = question; }
    public void setUser(User user) { this.user = user; }
    public void setOption(PollOption option) { this.option = option; }
    public void setTextAnswer(String textAnswer) { this.textAnswer = textAnswer; }
}

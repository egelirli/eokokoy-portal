package com.ekokoy.portal.poll.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "poll_options")
@EntityListeners(AuditingEntityListener.class)
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private PollQuestion question;

    @Column(name = "option_text", nullable = false, length = 500)
    private String optionText;

    @Column(name = "option_order", nullable = false)
    private int optionOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public PollQuestion getQuestion() { return question; }
    public String getOptionText() { return optionText; }
    public int getOptionOrder() { return optionOrder; }
    public Instant getCreatedAt() { return createdAt; }

    public void setQuestion(PollQuestion question) { this.question = question; }
    public void setOptionText(String optionText) { this.optionText = optionText; }
    public void setOptionOrder(int optionOrder) { this.optionOrder = optionOrder; }
}

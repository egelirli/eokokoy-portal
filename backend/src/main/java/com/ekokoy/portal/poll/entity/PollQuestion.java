package com.ekokoy.portal.poll.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "poll_questions")
@EntityListeners(AuditingEntityListener.class)
public class PollQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired = true;

    @Column(name = "question_order", nullable = false)
    private int questionOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionOrder ASC")
    private List<PollOption> options = new ArrayList<>();

    public UUID getId() { return id; }
    public Poll getPoll() { return poll; }
    public String getQuestionText() { return questionText; }
    public QuestionType getQuestionType() { return questionType; }
    public boolean isRequired() { return isRequired; }
    public int getQuestionOrder() { return questionOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public List<PollOption> getOptions() { return options; }

    public void setId(UUID id) { this.id = id; }
    public void setPoll(Poll poll) { this.poll = poll; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
    public void setRequired(boolean required) { isRequired = required; }
    public void setQuestionOrder(int questionOrder) { this.questionOrder = questionOrder; }
    public void setOptions(List<PollOption> options) { this.options = options; }
}

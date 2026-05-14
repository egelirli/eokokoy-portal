package com.ekokoy.portal.poll.dto;

import com.ekokoy.portal.poll.entity.PollQuestion;
import com.ekokoy.portal.poll.entity.QuestionType;

import java.util.List;
import java.util.UUID;

public record PollQuestionResponse(
        UUID id,
        String questionText,
        QuestionType questionType,
        boolean isRequired,
        int questionOrder,
        List<PollOptionResponse> options
) {
    public static PollQuestionResponse from(PollQuestion q) {
        List<PollOptionResponse> opts = q.getOptions().stream()
                .map(PollOptionResponse::from).toList();
        return new PollQuestionResponse(
                q.getId(), q.getQuestionText(), q.getQuestionType(),
                q.isRequired(), q.getQuestionOrder(), opts);
    }
}

package com.ekokoy.portal.poll.dto;

import com.ekokoy.portal.poll.entity.QuestionType;

import java.util.List;
import java.util.UUID;

public record QuestionResultResponse(
        UUID questionId,
        String questionText,
        QuestionType questionType,
        int questionOrder,
        long totalAnswers,
        List<OptionResultResponse> optionResults,
        List<String> textAnswers
) {}

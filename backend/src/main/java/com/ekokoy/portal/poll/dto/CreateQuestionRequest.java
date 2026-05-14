package com.ekokoy.portal.poll.dto;

import com.ekokoy.portal.poll.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateQuestionRequest(
        @NotBlank String questionText,
        @NotNull QuestionType questionType,
        boolean isRequired,
        int questionOrder,
        @Valid List<CreateOptionRequest> options
) {}

package com.ekokoy.portal.poll.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AnswerRequest(
        @NotNull UUID questionId,
        UUID optionId,
        List<UUID> optionIds,
        String textAnswer
) {}

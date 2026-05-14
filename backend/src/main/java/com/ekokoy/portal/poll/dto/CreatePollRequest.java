package com.ekokoy.portal.poll.dto;

import com.ekokoy.portal.poll.entity.PollType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreatePollRequest(
        @NotNull PollType type,
        @NotBlank @Size(max = 255) String title,
        String description,
        boolean isAnonymous,
        @NotEmpty List<String> eligibleRoles,
        @NotNull Instant startsAt,
        Instant endsAt,
        @NotEmpty @Valid List<CreateQuestionRequest> questions
) {}

package com.ekokoy.portal.poll.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitResponseRequest(
        @NotEmpty @Valid List<AnswerRequest> answers
) {}

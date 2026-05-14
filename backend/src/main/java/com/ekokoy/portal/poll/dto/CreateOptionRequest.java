package com.ekokoy.portal.poll.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOptionRequest(
        @NotBlank String optionText,
        int optionOrder
) {}

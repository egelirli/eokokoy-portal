package com.ekokoy.portal.poll.dto;

import java.util.UUID;

public record OptionResultResponse(
        UUID optionId,
        String optionText,
        long count,
        double percentage
) {}

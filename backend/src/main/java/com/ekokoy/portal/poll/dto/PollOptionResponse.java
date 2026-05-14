package com.ekokoy.portal.poll.dto;

import com.ekokoy.portal.poll.entity.PollOption;

import java.util.UUID;

public record PollOptionResponse(
        UUID id,
        String optionText,
        int optionOrder
) {
    public static PollOptionResponse from(PollOption o) {
        return new PollOptionResponse(o.getId(), o.getOptionText(), o.getOptionOrder());
    }
}

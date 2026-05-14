package com.ekokoy.portal.poll.dto;

import java.util.UUID;

public record ParticipantResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        boolean hasVoted
) {}

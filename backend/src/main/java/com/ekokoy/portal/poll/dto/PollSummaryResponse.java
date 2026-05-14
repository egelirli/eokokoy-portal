package com.ekokoy.portal.poll.dto;

import com.ekokoy.portal.poll.entity.Poll;
import com.ekokoy.portal.poll.entity.PollStatus;
import com.ekokoy.portal.poll.entity.PollType;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record PollSummaryResponse(
        UUID id,
        PollType type,
        String title,
        PollStatus status,
        boolean isAnonymous,
        List<String> eligibleRoles,
        Instant startsAt,
        Instant endsAt,
        Instant createdAt,
        boolean hasResponded
) {
    public static PollSummaryResponse from(Poll poll, boolean hasResponded) {
        return new PollSummaryResponse(
                poll.getId(), poll.getType(), poll.getTitle(), poll.getStatus(),
                poll.isAnonymous(), Arrays.asList(poll.getEligibleRoles()),
                poll.getStartsAt(), poll.getEndsAt(), poll.getCreatedAt(), hasResponded);
    }
}

package com.ekokoy.portal.poll.dto;

import com.ekokoy.portal.poll.entity.Poll;
import com.ekokoy.portal.poll.entity.PollStatus;
import com.ekokoy.portal.poll.entity.PollType;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record PollDetailResponse(
        UUID id,
        PollType type,
        String title,
        String description,
        PollStatus status,
        boolean isAnonymous,
        List<String> eligibleRoles,
        Instant startsAt,
        Instant endsAt,
        UUID createdById,
        String createdByName,
        Instant createdAt,
        List<PollQuestionResponse> questions,
        boolean hasResponded
) {
    public static PollDetailResponse from(Poll poll, boolean hasResponded) {
        List<PollQuestionResponse> questions = poll.getQuestions().stream()
                .map(PollQuestionResponse::from).toList();
        String creatorName = poll.getCreatedBy().getFirstName() + " " + poll.getCreatedBy().getLastName();
        return new PollDetailResponse(
                poll.getId(), poll.getType(), poll.getTitle(), poll.getDescription(),
                poll.getStatus(), poll.isAnonymous(),
                Arrays.asList(poll.getEligibleRoles()),
                poll.getStartsAt(), poll.getEndsAt(),
                poll.getCreatedBy().getId(), creatorName,
                poll.getCreatedAt(), questions, hasResponded);
    }
}

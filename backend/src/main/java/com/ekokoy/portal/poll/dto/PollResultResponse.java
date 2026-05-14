package com.ekokoy.portal.poll.dto;

import com.ekokoy.portal.poll.entity.PollStatus;
import com.ekokoy.portal.poll.entity.PollType;

import java.util.List;
import java.util.UUID;

public record PollResultResponse(
        UUID pollId,
        String title,
        PollType type,
        PollStatus status,
        boolean isAnonymous,
        long totalParticipants,
        long totalEligible,
        double participationRate,
        List<QuestionResultResponse> questionResults
) {}

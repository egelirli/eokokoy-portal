package com.ekokoy.portal.poll.repository;

import com.ekokoy.portal.poll.entity.PollQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PollQuestionRepository extends JpaRepository<PollQuestion, UUID> {

    List<PollQuestion> findByPollIdOrderByQuestionOrderAsc(UUID pollId);
}

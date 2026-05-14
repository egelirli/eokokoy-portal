package com.ekokoy.portal.poll.repository;

import com.ekokoy.portal.poll.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {

    List<PollOption> findByQuestionIdOrderByOptionOrderAsc(UUID questionId);
}

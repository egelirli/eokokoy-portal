package com.ekokoy.portal.poll.repository;

import com.ekokoy.portal.poll.entity.PollResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PollResponseRepository extends JpaRepository<PollResponse, UUID> {

    /** Kullanıcının belirli bir ankete yanıt verip vermediğini kontrol eder. */
    boolean existsByPollIdAndUserId(UUID pollId, UUID userId);

    /** Anketteki tüm yanıtları döner. */
    List<PollResponse> findByPollId(UUID pollId);

    /** Ankete yanıt veren benzersiz kullanıcı ID'lerini döner. */
    @Query("SELECT DISTINCT r.user.id FROM PollResponse r WHERE r.poll.id = :pollId")
    List<UUID> findDistinctUserIdsByPollId(UUID pollId);

    /** Belirli bir soru için tüm yanıtları döner. */
    List<PollResponse> findByQuestionId(UUID questionId);

    /** Belirli bir kullanıcının ankete verdiği yanıtları döner. */
    List<PollResponse> findByPollIdAndUserId(UUID pollId, UUID userId);
}

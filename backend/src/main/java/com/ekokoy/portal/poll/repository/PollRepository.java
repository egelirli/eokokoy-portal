package com.ekokoy.portal.poll.repository;

import com.ekokoy.portal.poll.entity.Poll;
import com.ekokoy.portal.poll.entity.PollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PollRepository extends JpaRepository<Poll, UUID> {

    /** Belirli bir status'teki tüm anketleri döner. */
    List<Poll> findByStatus(PollStatus status);

    /** starts_at zamanı geçmiş ve hâlâ draft olan anketleri döner (aktifleştirme için). */
    @Query("SELECT p FROM Poll p WHERE p.status = 'draft' AND p.startsAt <= :now")
    List<Poll> findDraftPollsReadyToActivate(Instant now);

    /** ends_at zamanı geçmiş ve hâlâ active olan anketleri döner (kapatma için). */
    @Query("SELECT p FROM Poll p WHERE p.status = 'active' AND p.endsAt IS NOT NULL AND p.endsAt <= :now")
    List<Poll> findActivePollsReadyToClose(Instant now);

    /** Tüm anketleri en yeni önce listeler. */
    List<Poll> findAllByOrderByCreatedAtDesc();
}

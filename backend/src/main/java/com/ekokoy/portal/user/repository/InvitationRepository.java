package com.ekokoy.portal.user.repository;

import com.ekokoy.portal.user.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    @Query("SELECT i FROM Invitation i JOIN FETCH i.role WHERE i.token = :tokenHash")
    Optional<Invitation> findByTokenWithRole(String tokenHash);

    boolean existsByEmailAndIsUsedFalse(String email);
}

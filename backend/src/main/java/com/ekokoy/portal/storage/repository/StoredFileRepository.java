package com.ekokoy.portal.storage.repository;

import com.ekokoy.portal.storage.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    /** Aynı checksum'a sahip, onaylanmış ve silinmemiş dosyayı getirir (duplikat tespiti). */
    Optional<StoredFile> findByChecksumAndIsConfirmedTrueAndIsDeletedFalse(String checksum);

    /** Onaylanmamış ve belirtilen tarihten önce yüklenmiş, silinmemiş kayıtları getirir (cleanup). */
    @Query("SELECT f FROM StoredFile f WHERE f.isConfirmed = false AND f.isDeleted = false AND f.uploadedAt < :cutoff")
    List<StoredFile> findUnconfirmedBefore(@Param("cutoff") Instant cutoff);
}

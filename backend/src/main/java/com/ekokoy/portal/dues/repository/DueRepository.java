package com.ekokoy.portal.dues.repository;

import com.ekokoy.portal.dues.entity.Due;
import com.ekokoy.portal.dues.entity.DueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DueRepository extends JpaRepository<Due, UUID> {

    @Query("SELECT d FROM Due d JOIN FETCH d.property WHERE d.property.id = :propertyId ORDER BY d.periodYear DESC, d.periodMonth DESC NULLS LAST")
    List<Due> findByPropertyId(@Param("propertyId") UUID propertyId);

    @Query("SELECT d FROM Due d JOIN FETCH d.property WHERE d.property.id IN :propertyIds ORDER BY d.periodYear DESC, d.periodMonth DESC NULLS LAST")
    List<Due> findByPropertyIdIn(@Param("propertyIds") List<UUID> propertyIds);

    @Query("SELECT d FROM Due d JOIN FETCH d.property ORDER BY d.periodYear DESC, d.periodMonth DESC NULLS LAST")
    List<Due> findAllWithProperty();

    /** Aylık aidat için upsert bulma. */
    @Query("SELECT d FROM Due d WHERE d.property.id = :propertyId AND d.periodYear = :year AND d.periodMonth = :month")
    Optional<Due> findByPropertyIdAndYearAndMonth(
            @Param("propertyId") UUID propertyId,
            @Param("year") int year,
            @Param("month") int month
    );

    /** Yıllık aidat için upsert bulma (ay null). */
    @Query("SELECT d FROM Due d WHERE d.property.id = :propertyId AND d.periodYear = :year AND d.periodMonth IS NULL")
    Optional<Due> findByPropertyIdAndYearAndNullMonth(
            @Param("propertyId") UUID propertyId,
            @Param("year") int year
    );

    /** Hatırlatma: 3 gün önce vadesi gelecek borçlar. */
    @Query("SELECT d FROM Due d JOIN FETCH d.property WHERE d.dueDate = :targetDate AND d.status IN ('unpaid', 'partially_paid')")
    List<Due> findByDueDateAndStatusUnpaid(@Param("targetDate") LocalDate targetDate);

    /** Hatırlatma: vadesi geçmiş borçlar. */
    @Query("SELECT d FROM Due d JOIN FETCH d.property WHERE d.dueDate < :today AND d.status IN ('unpaid', 'partially_paid')")
    List<Due> findOverdue(@Param("today") LocalDate today);
}

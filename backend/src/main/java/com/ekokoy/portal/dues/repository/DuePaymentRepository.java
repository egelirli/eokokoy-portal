package com.ekokoy.portal.dues.repository;

import com.ekokoy.portal.dues.entity.DuePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface DuePaymentRepository extends JpaRepository<DuePayment, UUID> {

    @Query("SELECT p FROM DuePayment p JOIN FETCH p.recordedBy WHERE p.due.id = :dueId ORDER BY p.paymentDate DESC")
    List<DuePayment> findByDueId(@Param("dueId") UUID dueId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM DuePayment p WHERE p.due.id = :dueId")
    BigDecimal sumAmountByDueId(@Param("dueId") UUID dueId);
}

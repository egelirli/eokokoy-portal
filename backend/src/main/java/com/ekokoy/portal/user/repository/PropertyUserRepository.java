package com.ekokoy.portal.user.repository;

import com.ekokoy.portal.user.entity.PropertyUser;
import com.ekokoy.portal.user.entity.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PropertyUserRepository extends JpaRepository<PropertyUser, UUID> {

    @Query("SELECT pu FROM PropertyUser pu JOIN FETCH pu.user WHERE pu.property.id = :propertyId AND pu.endDate IS NULL")
    List<PropertyUser> findActiveByPropertyId(@Param("propertyId") UUID propertyId);

    @Query("SELECT pu FROM PropertyUser pu JOIN FETCH pu.user WHERE pu.property.id = :propertyId ORDER BY pu.createdAt DESC")
    List<PropertyUser> findAllByPropertyId(@Param("propertyId") UUID propertyId);

    @Query("SELECT pu FROM PropertyUser pu JOIN FETCH pu.property WHERE pu.user.id = :userId AND pu.endDate IS NULL")
    List<PropertyUser> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(pu) FROM PropertyUser pu WHERE pu.property.id = :propertyId AND pu.relationType = :relationType AND pu.endDate IS NULL")
    long countActiveByPropertyIdAndRelationType(@Param("propertyId") UUID propertyId, @Param("relationType") RelationType relationType);

    @Query("SELECT COALESCE(SUM(pu.ownershipPercentage), 0) FROM PropertyUser pu WHERE pu.property.id = :propertyId AND pu.relationType = :relationType AND pu.endDate IS NULL")
    BigDecimal sumOwnershipPercentageByPropertyIdAndRelationType(@Param("propertyId") UUID propertyId, @Param("relationType") RelationType relationType);

    @Query("SELECT COUNT(pu) FROM PropertyUser pu WHERE pu.user.id = :userId AND pu.relationType = :relationType AND pu.endDate IS NULL")
    long countActiveByUserIdAndRelationType(@Param("userId") UUID userId, @Param("relationType") RelationType relationType);
}

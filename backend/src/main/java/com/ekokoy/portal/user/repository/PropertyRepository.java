package com.ekokoy.portal.user.repository;

import com.ekokoy.portal.user.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    Optional<Property> findByUnitNumber(String unitNumber);

    boolean existsByUnitNumber(String unitNumber);
}

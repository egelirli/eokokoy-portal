package com.ekokoy.portal.dues.repository;

import com.ekokoy.portal.dues.entity.DueImport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DueImportRepository extends JpaRepository<DueImport, UUID> {
}

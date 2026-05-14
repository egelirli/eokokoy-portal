package com.ekokoy.portal.user.repository;

import com.ekokoy.portal.user.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    List<Permission> findAllByCategory(String category);
}

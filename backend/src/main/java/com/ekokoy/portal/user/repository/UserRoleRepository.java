package com.ekokoy.portal.user.repository;

import com.ekokoy.portal.user.entity.UserRole;
import com.ekokoy.portal.user.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role WHERE ur.user.id = :userId")
    List<UserRole> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.id = :roleId")
    boolean existsByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);
}

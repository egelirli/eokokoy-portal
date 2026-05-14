package com.ekokoy.portal.user.repository;

import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmailAndIsDeletedFalse(String email);

    List<User> findAllByIsDeletedFalse();

    List<User> findAllByStatusAndIsDeletedFalse(UserStatus status);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role r LEFT JOIN FETCH r.permissions WHERE u.id = :id AND u.isDeleted = false")
    Optional<User> findByIdWithRolesAndPermissions(UUID id);
}

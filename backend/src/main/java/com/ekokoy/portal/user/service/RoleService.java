package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.user.dto.*;
import com.ekokoy.portal.user.entity.Role;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserRole;
import com.ekokoy.portal.user.entity.UserRoleId;
import com.ekokoy.portal.user.repository.RoleRepository;
import com.ekokoy.portal.user.repository.UserRepository;
import com.ekokoy.portal.user.repository.UserRoleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RoleService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public RoleService(RoleRepository roleRepository,
                       UserRepository userRepository,
                       UserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /** Aktif tüm rolleri listeler. */
    public List<RoleResponse> listRoles() {
        return roleRepository.findAllByIsActiveTrue().stream()
                .map(RoleResponse::from)
                .toList();
    }

    /** Belirtilen rolün izinlerini döner. */
    public RoleWithPermissionsResponse getRolePermissions(UUID roleId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new EkokoyException("ROLE_NOT_FOUND", "Rol bulunamadı.", 404));
        return RoleWithPermissionsResponse.from(role);
    }

    /** Kullanıcının sahip olduğu rolleri listeler. */
    public List<UserRoleResponse> getUserRoles(UUID userId) {
        ensureUserExists(userId);
        return userRoleRepository.findAllByUserId(userId).stream()
                .map(UserRoleResponse::from)
                .toList();
    }

    /** Kullanıcıya rol atar. İş kuralları: SUPER_ADMIN kısıtlamaları uygulanır. */
    @Transactional
    public UserRoleResponse assignRole(UUID targetUserId, AssignRoleRequest request) {
        User targetUser = ensureUserExists(targetUserId);
        Role roleToAssign = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new EkokoyException("ROLE_NOT_FOUND", "Rol bulunamadı.", 404));

        UUID actorId = currentUserId();
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new EkokoyException("ACTOR_NOT_FOUND", "İşlemi yapan kullanıcı bulunamadı.", 404));

        // SUPER_ADMIN rolünü sadece mevcut SUPER_ADMIN atayabilir
        if (SUPER_ADMIN.equals(roleToAssign.getCode()) && !isCurrentUserSuperAdmin()) {
            throw new EkokoyException(
                    "FORBIDDEN_ROLE_ASSIGN",
                    "SUPER_ADMIN rolünü sadece mevcut SUPER_ADMIN atayabilir.",
                    403
            );
        }

        // SUPER_ADMIN başka rolle birleştirilemez
        if (SUPER_ADMIN.equals(roleToAssign.getCode())) {
            long existingRoles = userRoleRepository.countByUserId(targetUserId);
            if (existingRoles > 0) {
                throw new EkokoyException(
                        "SUPER_ADMIN_CONFLICT",
                        "SUPER_ADMIN rolü başka rollerle birleştirilemez. Önce mevcut rolleri kaldırın.",
                        422
                );
            }
        }

        // Hedef kullanıcının zaten SUPER_ADMIN rolü varsa başka rol atanamaz
        boolean targetIsSuperAdmin = userRoleRepository.findAllByUserId(targetUserId).stream()
                .anyMatch(ur -> SUPER_ADMIN.equals(ur.getRole().getCode()));
        if (targetIsSuperAdmin && !SUPER_ADMIN.equals(roleToAssign.getCode())) {
            throw new EkokoyException(
                    "SUPER_ADMIN_CONFLICT",
                    "SUPER_ADMIN kullanıcısına başka rol atanamaz.",
                    422
            );
        }

        // Aynı rol zaten atanmış mı?
        if (userRoleRepository.existsByUserIdAndRoleId(targetUserId, roleToAssign.getId())) {
            throw new EkokoyException(
                    "ROLE_ALREADY_ASSIGNED",
                    "Kullanıcı zaten bu role sahip.",
                    409
            );
        }

        UserRole userRole = new UserRole(targetUser, roleToAssign, actor);
        userRoleRepository.save(userRole);
        return UserRoleResponse.from(userRole);
    }

    /** Kullanıcıdan rol kaldırır. Son rol kaldırılamaz. */
    @Transactional
    public void removeRole(UUID targetUserId, UUID roleId) {
        ensureUserExists(targetUserId);

        // SUPER_ADMIN rolünü sadece mevcut SUPER_ADMIN kaldırabilir
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EkokoyException("ROLE_NOT_FOUND", "Rol bulunamadı.", 404));
        if (SUPER_ADMIN.equals(role.getCode()) && !isCurrentUserSuperAdmin()) {
            throw new EkokoyException(
                    "FORBIDDEN_ROLE_REMOVE",
                    "SUPER_ADMIN rolünü sadece mevcut SUPER_ADMIN kaldırabilir.",
                    403
            );
        }

        // Kullanıcı bu role sahip mi?
        if (!userRoleRepository.existsByUserIdAndRoleId(targetUserId, roleId)) {
            throw new EkokoyException("ROLE_NOT_ASSIGNED", "Kullanıcı bu role sahip değil.", 404);
        }

        // Son rolü kaldıramazsın
        long roleCount = userRoleRepository.countByUserId(targetUserId);
        if (roleCount <= 1) {
            throw new EkokoyException(
                    "LAST_ROLE_REMOVAL",
                    "Kullanıcının son rolü kaldırılamaz.",
                    422
            );
        }

        userRoleRepository.deleteById(new UserRoleId(targetUserId, roleId));
    }

    /** Giriş yapmış kullanıcının sahip olduğu tüm izinlerin birleşimini döner. */
    public List<String> getMyPermissions() {
        UUID userId = currentUserId();
        return userRoleRepository.findAllByUserId(userId).stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(p -> p.getCode())
                .distinct()
                .sorted()
                .toList();
    }

    private User ensureUserExists(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new EkokoyException("USER_NOT_FOUND", "Kullanıcı bulunamadı.", 404));
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString((String) auth.getPrincipal());
    }

    private boolean isCurrentUserSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + SUPER_ADMIN));
    }
}

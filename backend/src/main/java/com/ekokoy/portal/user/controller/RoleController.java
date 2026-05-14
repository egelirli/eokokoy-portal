package com.ekokoy.portal.user.controller;

import com.ekokoy.portal.common.ApiResponse;
import com.ekokoy.portal.user.dto.*;
import com.ekokoy.portal.user.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /** Tüm aktif rolleri listeler. */
    @GetMapping("/admin/roles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> listRoles() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.listRoles()));
    }

    /** Belirtilen rolün izin listesini döner. */
    @GetMapping("/admin/roles/{id}/permissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<RoleWithPermissionsResponse>> getRolePermissions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getRolePermissions(id)));
    }

    /** Kullanıcının sahip olduğu rolleri listeler. */
    @GetMapping("/admin/users/{id}/roles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<UserRoleResponse>>> getUserRoles(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getUserRoles(id)));
    }

    /** Kullanıcıya yeni rol atar. */
    @PostMapping("/admin/users/{id}/roles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<UserRoleResponse>> assignRole(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRoleRequest request) {
        UserRoleResponse result = roleService.assignRole(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result, "Rol başarıyla atandı."));
    }

    /** Kullanıcıdan rol kaldırır. */
    @DeleteMapping("/admin/users/{id}/roles/{roleId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<Void>> removeRole(@PathVariable UUID id, @PathVariable UUID roleId) {
        roleService.removeRole(id, roleId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Rol başarıyla kaldırıldı."));
    }

    /** Giriş yapmış kullanıcının izin listesini döner. */
    @GetMapping("/me/permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<String>>> getMyPermissions() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getMyPermissions()));
    }
}

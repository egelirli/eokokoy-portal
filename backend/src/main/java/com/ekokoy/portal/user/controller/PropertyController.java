package com.ekokoy.portal.user.controller;

import com.ekokoy.portal.common.ApiResponse;
import com.ekokoy.portal.user.dto.*;
import com.ekokoy.portal.user.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    /** Tüm konutları listeler. */
    @GetMapping("/admin/properties")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> listProperties() {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.listProperties()));
    }

    /** Konut detayını ve aktif sakinleri döner. */
    @GetMapping("/admin/properties/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> getProperty(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.getProperty(id)));
    }

    /** Giriş yapmış kullanıcının konutlarını döner. */
    @GetMapping("/properties/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserPropertyResponse>>> getMyProperties() {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.getMyProperties()));
    }

    /** Konuta sakin ekler. */
    @PostMapping("/admin/properties/{id}/residents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<ResidentResponse>> addResident(
            @PathVariable UUID id,
            @Valid @RequestBody AddResidentRequest request) {
        ResidentResponse result = propertyService.addResident(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result, "Sakin başarıyla eklendi."));
    }

    /** Sakin-konut ilişkisini sonlandırır. */
    @PatchMapping("/admin/properties/{propertyId}/residents/{relationId}/end")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<ResidentResponse>> endRelation(
            @PathVariable UUID propertyId,
            @PathVariable UUID relationId) {
        ResidentResponse result = propertyService.endRelation(propertyId, relationId);
        return ResponseEntity.ok(ApiResponse.ok(result, "İlişki başarıyla sonlandırıldı."));
    }

    /** Konuttaki tüm sakin geçmişini döner. */
    @GetMapping("/admin/properties/{id}/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<ResidentResponse>>> getPropertyHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.getPropertyHistory(id)));
    }

    /** Bir kullanıcının aktif konut ilişkilerini döner. */
    @GetMapping("/admin/users/{id}/properties")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<UserPropertyResponse>>> getUserProperties(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.getUserProperties(id)));
    }
}

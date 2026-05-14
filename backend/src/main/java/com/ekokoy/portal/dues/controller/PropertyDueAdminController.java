package com.ekokoy.portal.dues.controller;

import com.ekokoy.portal.common.ApiResponse;
import com.ekokoy.portal.dues.dto.DueResponse;
import com.ekokoy.portal.dues.service.DueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/properties")
public class PropertyDueAdminController {

    private final DueService dueService;

    public PropertyDueAdminController(DueService dueService) {
        this.dueService = dueService;
    }

    /** Belirli bir konutun borçlarını listeler. */
    @GetMapping("/{id}/dues")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<DueResponse>>> getPropertyDues(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(dueService.getPropertyDues(id)));
    }
}

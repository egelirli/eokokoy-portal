package com.ekokoy.portal.user.controller;

import com.ekokoy.portal.common.ApiResponse;
import com.ekokoy.portal.user.dto.ApplicationResponse;
import com.ekokoy.portal.user.dto.UpdateApplicationRequest;
import com.ekokoy.portal.user.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /** Tüm başvuruları listeler. */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> listApplications() {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.listApplications()));
    }

    /** Başvuruyu onayla / reddet / bilgi iste. */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplication(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.updateApplication(id, request)));
    }
}

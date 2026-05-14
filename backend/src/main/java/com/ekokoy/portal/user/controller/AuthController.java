package com.ekokoy.portal.user.controller;

import com.ekokoy.portal.common.ApiResponse;
import com.ekokoy.portal.user.dto.ApplicationResponse;
import com.ekokoy.portal.user.dto.ApplyRequest;
import com.ekokoy.portal.user.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final ApplicationService applicationService;

    public AuthController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /** Yeni üyelik başvurusu — herkese açık. */
    @PostMapping("/apply")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(@Valid @RequestBody ApplyRequest request) {
        ApplicationResponse response = applicationService.apply(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response,
                "Başvurunuz alındı. Yönetim kurulu incelemesinin ardından e-posta ile bilgilendirileceksiniz."));
    }
}

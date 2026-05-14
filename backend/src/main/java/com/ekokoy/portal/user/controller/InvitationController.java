package com.ekokoy.portal.user.controller;

import com.ekokoy.portal.common.ApiResponse;
import com.ekokoy.portal.user.dto.*;
import com.ekokoy.portal.user.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    /** Yeni davet oluşturur. */
    @PostMapping("/api/v1/admin/invitations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<InvitationResponse>> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request) {
        InvitationResponse response = invitationService.createInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /** Daveti yeniden gönderir. */
    @PostMapping("/api/v1/admin/invitations/{id}/resend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<InvitationResponse>> resendInvitation(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.resendInvitation(id)));
    }

    /** Token doğrular — herkese açık. */
    @GetMapping("/api/v1/invitations/verify/{token}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<VerifyInvitationResponse>> verifyToken(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.verifyToken(token)));
    }

    /** Davet tamamlama (kayıt) — herkese açık. */
    @PostMapping("/api/v1/invitations/complete")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<ApplicationResponse>> completeInvitation(
            @Valid @RequestBody CompleteInvitationRequest request) {
        ApplicationResponse response = invitationService.completeInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response,
                "Kayıt tamamlandı. Sisteme giriş yapabilirsiniz."));
    }
}

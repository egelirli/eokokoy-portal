package com.ekokoy.portal.dues.controller;

import com.ekokoy.portal.common.ApiResponse;
import com.ekokoy.portal.dues.dto.DueResponse;
import com.ekokoy.portal.dues.dto.DueSummaryResponse;
import com.ekokoy.portal.dues.service.DueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dues")
public class DueController {

    private final DueService dueService;

    public DueController(DueService dueService) {
        this.dueService = dueService;
    }

    /** Giriş yapan ev sahibinin borçlarını listeler. */
    @GetMapping("/my")
    @PreAuthorize("hasRole('EV_SAHIBI')")
    public ResponseEntity<ApiResponse<List<DueResponse>>> getMyDues() {
        return ResponseEntity.ok(ApiResponse.ok(dueService.getMyDues()));
    }

    /** Giriş yapan ev sahibinin borç özetini döner. */
    @GetMapping("/my/summary")
    @PreAuthorize("hasRole('EV_SAHIBI')")
    public ResponseEntity<ApiResponse<DueSummaryResponse>> getMyDuesSummary() {
        return ResponseEntity.ok(ApiResponse.ok(dueService.getMyDuesSummary()));
    }
}

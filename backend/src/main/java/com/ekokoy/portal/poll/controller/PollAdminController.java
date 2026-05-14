package com.ekokoy.portal.poll.controller;

import com.ekokoy.portal.common.ApiResponse;
import com.ekokoy.portal.poll.dto.*;
import com.ekokoy.portal.poll.service.PollService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/polls")
public class PollAdminController {

    private final PollService pollService;

    public PollAdminController(PollService pollService) {
        this.pollService = pollService;
    }

    /** Tüm anketleri listeler. */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<PollSummaryResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(pollService.listAllPolls()));
    }

    /** Yeni anket oluşturur. */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<PollDetailResponse>> create(
            @Valid @RequestBody CreatePollRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(pollService.createPoll(req)));
    }

    /** Draft anketi günceller. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<PollDetailResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePollRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(pollService.updatePoll(id, req)));
    }

    /** Anketi aktifleştirir. */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<PollDetailResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(pollService.activatePoll(id)));
    }

    /** Anketi kapatır. */
    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<PollDetailResponse>> close(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(pollService.closePoll(id)));
    }

    /** Anketi iptal eder (SUPER_ADMIN). */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PollDetailResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(pollService.cancelPoll(id)));
    }

    /** Anketin tam sonuçlarını döner. */
    @GetMapping("/{id}/results/full")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<PollResultResponse>> fullResults(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(pollService.getFullResults(id)));
    }

    /** Ankete katılan ve katılmayan kullanıcıları listeler. */
    @GetMapping("/{id}/participants")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<ParticipantResponse>>> participants(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(pollService.getParticipants(id)));
    }
}

package com.ekokoy.portal.poll.controller;

import com.ekokoy.portal.common.ApiResponse;
import com.ekokoy.portal.poll.dto.*;
import com.ekokoy.portal.poll.service.PollService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/polls")
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    /** Giriş yapan kullanıcıya uygun aktif anketleri listeler. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PollSummaryResponse>>> listPolls() {
        return ResponseEntity.ok(ApiResponse.ok(pollService.listMyPolls()));
    }

    /** Anket detayını döner. */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PollDetailResponse>> getPoll(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(pollService.getPoll(id)));
    }

    /** Anket sonuçlarını döner (eligible_roles kısıtı uygulanır). */
    @GetMapping("/{id}/results")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PollResultResponse>> getResults(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(pollService.getResults(id)));
    }

    /** Ankete yanıt gönderir (eligible_roles kısıtı, tek yanıt). */
    @PostMapping("/{id}/respond")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> respond(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitResponseRequest req) {
        pollService.respond(id, req);
        return ResponseEntity.ok(ApiResponse.ok(null, "Yanıtınız kaydedildi."));
    }
}

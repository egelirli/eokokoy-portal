package com.ekokoy.portal.announcement.controller;

import com.ekokoy.portal.announcement.dto.*;
import com.ekokoy.portal.announcement.service.AnnouncementService;
import com.ekokoy.portal.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/announcements")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
public class AnnouncementAdminController {

    private final AnnouncementService announcementService;

    public AnnouncementAdminController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /** Yeni draft duyuru oluşturur. */
    @PostMapping
    public ResponseEntity<ApiResponse<AnnouncementResponse>> create(
            @Valid @RequestBody CreateAnnouncementRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(announcementService.createAnnouncement(req)));
    }

    /** Draft duyuruyu günceller. */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAnnouncementRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.updateAnnouncement(id, req)));
    }

    /** Draft duyuruyu yayına alır. */
    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.publish(id)));
    }

    /** Duyuruyu arşivler. */
    @PatchMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.archive(id)));
    }

    /** Draft duyuruya ek dosya ekler. */
    @PostMapping("/{id}/attachments")
    public ResponseEntity<ApiResponse<AttachmentResponse>> addAttachment(
            @PathVariable UUID id,
            @Valid @RequestBody AddAttachmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(announcementService.addAttachment(id, req)));
    }

    /** Draft duyurudan ek dosya siler. */
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {
        announcementService.deleteAttachment(id, attachmentId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Ek dosya silindi."));
    }

    /** Duyurunun okunma durumunu listeler. */
    @GetMapping("/{id}/read-status")
    public ResponseEntity<ApiResponse<List<ReadStatusResponse>>> getReadStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.getReadStatus(id)));
    }
}

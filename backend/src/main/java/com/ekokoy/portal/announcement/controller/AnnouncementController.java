package com.ekokoy.portal.announcement.controller;

import com.ekokoy.portal.announcement.dto.AnnouncementResponse;
import com.ekokoy.portal.announcement.service.AnnouncementService;
import com.ekokoy.portal.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /** Herkese açık yayınlanmış duyurular (public endpoint). */
    @GetMapping("/announcements/public")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getPublicAnnouncements() {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.getPublicAnnouncements()));
    }

    /** Yayınlanmış tüm duyurular (authenticated). */
    @GetMapping("/announcements")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getAnnouncements() {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.getAnnouncements()));
    }

    /** Tek duyuru (authenticated — okunma kaydı oluşturur). */
    @GetMapping("/announcements/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncement(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.getAnnouncement(id)));
    }
}

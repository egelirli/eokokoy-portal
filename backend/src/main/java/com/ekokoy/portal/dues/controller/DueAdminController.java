package com.ekokoy.portal.dues.controller;

import com.ekokoy.portal.common.ApiResponse;
import com.ekokoy.portal.dues.dto.*;
import com.ekokoy.portal.dues.service.DueImportService;
import com.ekokoy.portal.dues.service.DueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/dues")
public class DueAdminController {

    private final DueService dueService;
    private final DueImportService importService;

    public DueAdminController(DueService dueService, DueImportService importService) {
        this.dueService = dueService;
        this.importService = importService;
    }

    /** Tüm borçları listeler. */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<DueResponse>>> getAllDues() {
        return ResponseEntity.ok(ApiResponse.ok(dueService.getAllDues()));
    }

    /** Tüm borçların özetini döner. */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<DueSummaryResponse>> getAllDuesSummary() {
        return ResponseEntity.ok(ApiResponse.ok(dueService.getAllDuesSummary()));
    }

    /** Belirli bir konutun borçlarını listeler. */
    @GetMapping("/properties/{propertyId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<DueResponse>>> getPropertyDues(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(dueService.getPropertyDues(propertyId)));
    }

    /** CSV veya Excel dosyası ile borç yükler. */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<ImportDetailResponse>> importDues(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(importService.importFile(file)));
    }

    /** Import kayıtlarını listeler. */
    @GetMapping("/imports")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<ImportResponse>>> listImports() {
        return ResponseEntity.ok(ApiResponse.ok(importService.listImports()));
    }

    /** Import kaydının detayını döner. */
    @GetMapping("/imports/{importId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<ImportDetailResponse>> getImport(@PathVariable UUID importId) {
        return ResponseEntity.ok(ApiResponse.ok(importService.getImport(importId)));
    }

    /** Borca ödeme kaydeder. */
    @PostMapping("/{dueId}/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<DuePaymentResponse>> recordPayment(
            @PathVariable UUID dueId,
            @Valid @RequestBody RecordPaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dueService.recordPayment(dueId, req)));
    }

    /** Bir borcun ödemelerini listeler. */
    @GetMapping("/{dueId}/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','YONETIM_KURULU')")
    public ResponseEntity<ApiResponse<List<DuePaymentResponse>>> getPayments(@PathVariable UUID dueId) {
        return ResponseEntity.ok(ApiResponse.ok(dueService.getPayments(dueId)));
    }

    /** Ödeme kaydını siler (SUPER_ADMIN). */
    @DeleteMapping("/payments/{paymentId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable UUID paymentId) {
        dueService.deletePayment(paymentId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Ödeme kaydı silindi."));
    }

    /** Borcu iptal eder (SUPER_ADMIN). */
    @PatchMapping("/{dueId}/cancel")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DueResponse>> cancelDue(@PathVariable UUID dueId) {
        return ResponseEntity.ok(ApiResponse.ok(dueService.cancelDue(dueId)));
    }
}

package com.ekokoy.portal.storage.controller;

import com.ekokoy.portal.common.ApiResponse;
import com.ekokoy.portal.storage.dto.ConfirmUploadRequest;
import com.ekokoy.portal.storage.dto.FileResponse;
import com.ekokoy.portal.storage.dto.UploadUrlRequest;
import com.ekokoy.portal.storage.dto.UploadUrlResponse;
import com.ekokoy.portal.storage.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    /** Presigned PUT URL üretir; 15 dakika geçerli. */
    @PostMapping("/upload-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadUrlResponse>> generateUploadUrl(
            @Valid @RequestBody UploadUrlRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(storageService.generateUploadUrl(req)));
    }

    /** Yüklenen dosyayı onaylar; checksum duplikat kontrolü yapılır. */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FileResponse>> confirmUpload(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmUploadRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(storageService.confirmUpload(id, req)));
    }

    /** Dosya bilgisini ve geçici erişim URL'ini döndürür. */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FileResponse>> getFile(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(storageService.getFile(id)));
    }

    /** Dosyayı soft-delete yapar; MinIO'dan fiziksel silme yapılmaz. */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable UUID id) {
        storageService.deleteFile(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}

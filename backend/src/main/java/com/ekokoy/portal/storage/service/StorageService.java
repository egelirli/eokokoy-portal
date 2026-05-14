package com.ekokoy.portal.storage.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.config.MinioProperties;
import com.ekokoy.portal.storage.dto.ConfirmUploadRequest;
import com.ekokoy.portal.storage.dto.FileResponse;
import com.ekokoy.portal.storage.dto.UploadUrlRequest;
import com.ekokoy.portal.storage.dto.UploadUrlResponse;
import com.ekokoy.portal.storage.entity.FileType;
import com.ekokoy.portal.storage.entity.StoredFile;
import com.ekokoy.portal.storage.repository.StoredFileRepository;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.repository.UserRepository;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.*;
import io.minio.http.Method;
import org.apache.tika.Tika;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class StorageService {

    private static final int PRESIGNED_PUT_EXPIRY_MINUTES = 15;
    private static final int PRESIGNED_GET_EXPIRY_HOURS = 1;
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;   // 10 MB
    private static final long MAX_DOCUMENT_BYTES = 20L * 1024 * 1024; // 20 MB
    private static final int TIKA_READ_BYTES = 8192;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"
    );
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final StoredFileRepository fileRepository;
    private final UserRepository userRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final ImageProcessingService imageProcessingService;

    public StorageService(StoredFileRepository fileRepository,
                          UserRepository userRepository,
                          MinioClient minioClient,
                          MinioProperties minioProperties,
                          ImageProcessingService imageProcessingService) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.imageProcessingService = imageProcessingService;
    }

    /**
     * Dosya yüklemek için presigned PUT URL üretir ve bekleyen bir dosya kaydı oluşturur.
     */
    @Transactional
    public UploadUrlResponse generateUploadUrl(UploadUrlRequest req) {
        validateBucket(req.bucket());
        validateMimeType(req.mimeType());
        if (req.fileSize() != null) {
            validateFileSize(req.mimeType(), req.fileSize());
        }

        FileType fileType = resolveFileType(req.mimeType());
        String ext = extractExtension(req.originalName());
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        String objectKey = String.format("%s/%d/%02d/%s.%s",
                req.module(), now.getYear(), now.getMonthValue(), UUID.randomUUID(), ext);

        User uploader = userRepository.getReferenceById(currentUserId());
        StoredFile file = new StoredFile();
        file.setBucket(req.bucket());
        file.setObjectKey(objectKey);
        file.setOriginalName(req.originalName());
        file.setMimeType(req.mimeType());
        file.setFileSize(req.fileSize());
        file.setFileType(fileType);
        file.setUploadedBy(uploader);
        file.setUploadedAt(Instant.now());
        file = fileRepository.save(file);

        String presignedUrl = buildPresignedPutUrl(req.bucket(), objectKey);
        Instant expiresAt = Instant.now().plus(PRESIGNED_PUT_EXPIRY_MINUTES, ChronoUnit.MINUTES);

        return new UploadUrlResponse(file.getId(), presignedUrl, objectKey, expiresAt);
    }

    /**
     * Yüklenen dosyayı onaylar; duplikat checksum varsa mevcut dosyayı döndürür.
     * Fotoğrafsa async sıkıştırma + thumbnail tetikler.
     */
    @Transactional
    public FileResponse confirmUpload(UUID fileId, ConfirmUploadRequest req) {
        StoredFile file = requireFile(fileId);
        if (file.isDeleted()) {
            throw new EkokoyException("FILE_DELETED", "Bu dosya silindi.", 410);
        }
        if (file.isConfirmed()) {
            throw new EkokoyException("ALREADY_CONFIRMED", "Dosya zaten onaylanmış.", 422);
        }
        if (!file.getUploadedBy().getId().equals(currentUserId())) {
            throw new EkokoyException("FORBIDDEN", "Bu dosyayı onaylama yetkiniz yok.", 403);
        }

        verifyExistsInMinio(file);

        String detectedMime = detectMimeType(file);
        file.setMimeType(detectedMime);
        file.setFileType(resolveFileType(detectedMime));
        validateMimeType(detectedMime);
        if (file.getFileSize() != null) {
            validateFileSize(detectedMime, file.getFileSize());
        }

        // Duplikat kontrolü
        String checksum = req.checksum().toLowerCase();
        return fileRepository.findByChecksumAndIsConfirmedTrueAndIsDeletedFalse(checksum)
                .map(existing -> {
                    // Duplikat: bekleyen kaydı soft-delete, mevcut dosyayı döndür
                    file.setDeleted(true);
                    file.setDeletedAt(Instant.now());
                    fileRepository.save(file);
                    return buildFileResponse(existing);
                })
                .orElseGet(() -> {
                    file.setChecksum(checksum);
                    file.setConfirmed(true);
                    StoredFile saved = fileRepository.save(file);
                    if (saved.getFileType() == FileType.image) {
                        imageProcessingService.processAsync(saved.getId());
                    }
                    return buildFileResponse(saved);
                });
    }

    /**
     * Dosya bilgisini ve erişim URL'ini döndürür.
     * Bucket erişim matrisine göre yetki kontrolü yapılır.
     */
    @Transactional(readOnly = true)
    public FileResponse getFile(UUID fileId) {
        StoredFile file = requireFile(fileId);
        if (file.isDeleted()) {
            throw new EkokoyException("FILE_NOT_FOUND", "Dosya bulunamadı.", 404);
        }
        checkBucketReadAccess(file.getBucket(), file.getUploadedBy().getId());
        return buildFileResponse(file);
    }

    /**
     * Dosyayı soft-delete yapar. MinIO'dan fiziksel silme yapılmaz.
     */
    @Transactional
    public void deleteFile(UUID fileId) {
        StoredFile file = requireFile(fileId);
        if (file.isDeleted()) {
            throw new EkokoyException("FILE_NOT_FOUND", "Dosya bulunamadı.", 404);
        }
        UUID callerId = currentUserId();
        if (!file.getUploadedBy().getId().equals(callerId) && !isAdminOrYK()) {
            throw new EkokoyException("FORBIDDEN", "Bu dosyayı silme yetkiniz yok.", 403);
        }
        User deleter = userRepository.getReferenceById(callerId);
        file.setDeleted(true);
        file.setDeletedAt(Instant.now());
        file.setDeletedBy(deleter);
        fileRepository.save(file);
    }

    // ── Erişim kontrolü ──────────────────────────────────────────────────────────

    private void checkBucketReadAccess(String bucket, UUID ownerId) {
        if (bucket.equals(minioProperties.getAdminBucket()) && !isAdminOrYK()) {
            throw new EkokoyException("FORBIDDEN", "Bu dosyaya erişim yetkiniz yok.", 403);
        }
        if (bucket.equals(minioProperties.getProfilesBucket())) {
            if (!currentUserId().equals(ownerId) && !isAdminOrYK()) {
                throw new EkokoyException("FORBIDDEN", "Bu dosyaya erişim yetkiniz yok.", 403);
            }
        }
    }

    // ── MinIO yardımcıları ───────────────────────────────────────────────────────

    private void verifyExistsInMinio(StoredFile file) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(file.getBucket())
                    .object(file.getObjectKey())
                    .build());
        } catch (ErrorResponseException e) {
            throw new EkokoyException("FILE_NOT_UPLOADED",
                    "Dosya MinIO'ya yüklenmemiş veya hata oluştu.", 422);
        } catch (Exception e) {
            throw new EkokoyException("MINIO_ERROR", "MinIO bağlantı hatası.", 500);
        }
    }

    private String detectMimeType(StoredFile file) {
        try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                .bucket(file.getBucket())
                .object(file.getObjectKey())
                .offset(0L)
                .length((long) TIKA_READ_BYTES)
                .build())) {
            return new Tika().detect(is, file.getOriginalName());
        } catch (Exception e) {
            // Tika tespiti başarısız olursa iddia edilen MIME tipine geri dön
            return file.getMimeType();
        }
    }

    private String buildPresignedPutUrl(String bucket, String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(PRESIGNED_PUT_EXPIRY_MINUTES, TimeUnit.MINUTES)
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException
                 | InvalidKeyException | InvalidResponseException | IOException
                 | NoSuchAlgorithmException | XmlParserException | ServerException e) {
            throw new EkokoyException("MINIO_ERROR", "Presigned URL oluşturulamadı.", 500);
        }
    }

    String buildPresignedGetUrl(String bucket, String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(PRESIGNED_GET_EXPIRY_HOURS, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            return null;
        }
    }

    // ── FileResponse builder ─────────────────────────────────────────────────────

    FileResponse buildFileResponse(StoredFile file) {
        String accessUrl = resolveAccessUrl(file.getBucket(), file.getObjectKey());
        String thumbnailUrl = file.getThumbnailKey() != null
                ? resolveAccessUrl(file.getBucket(), file.getThumbnailKey()) : null;
        String compressedUrl = file.getCompressedKey() != null
                ? resolveAccessUrl(file.getBucket(), file.getCompressedKey()) : null;
        return FileResponse.from(file, accessUrl, thumbnailUrl, compressedUrl);
    }

    private String resolveAccessUrl(String bucket, String objectKey) {
        if (bucket.equals(minioProperties.getPublicBucket())) {
            return minioProperties.getEndpoint() + "/" + bucket + "/" + objectKey;
        }
        return buildPresignedGetUrl(bucket, objectKey);
    }

    // ── Doğrulama yardımcıları ───────────────────────────────────────────────────

    private void validateBucket(String bucket) {
        List<String> validBuckets = List.of(
                minioProperties.getPublicBucket(),
                minioProperties.getPrivateBucket(),
                minioProperties.getAdminBucket(),
                minioProperties.getProfilesBucket()
        );
        if (!validBuckets.contains(bucket)) {
            throw new EkokoyException("INVALID_BUCKET", "Geçersiz bucket: " + bucket, 422);
        }
        if (bucket.equals(minioProperties.getAdminBucket()) && !isAdminOrYK()) {
            throw new EkokoyException("FORBIDDEN", "Bu bucket'a yükleme yetkiniz yok.", 403);
        }
    }

    private void validateMimeType(String mimeType) {
        if (!ALLOWED_IMAGE_TYPES.contains(mimeType) && !ALLOWED_DOCUMENT_TYPES.contains(mimeType)) {
            throw new EkokoyException("INVALID_MIME_TYPE",
                    "Desteklenmeyen dosya türü: " + mimeType, 422);
        }
    }

    private void validateFileSize(String mimeType, long fileSize) {
        long limit = ALLOWED_IMAGE_TYPES.contains(mimeType) ? MAX_IMAGE_BYTES : MAX_DOCUMENT_BYTES;
        if (fileSize > limit) {
            String mb = (limit / 1024 / 1024) + " MB";
            throw new EkokoyException("FILE_TOO_LARGE", "Dosya boyutu " + mb + "'ı aşamaz.", 422);
        }
    }

    private FileType resolveFileType(String mimeType) {
        if (ALLOWED_IMAGE_TYPES.contains(mimeType)) return FileType.image;
        if (ALLOWED_DOCUMENT_TYPES.contains(mimeType)) return FileType.document;
        return FileType.other;
    }

    private String extractExtension(String originalName) {
        int dot = originalName.lastIndexOf('.');
        return dot >= 0 ? originalName.substring(dot + 1).toLowerCase() : "bin";
    }

    private StoredFile requireFile(UUID id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new EkokoyException("FILE_NOT_FOUND", "Dosya bulunamadı: " + id, 404));
    }

    private UUID currentUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private boolean isAdminOrYK() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equals("ROLE_YONETIM_KURULU"));
    }
}

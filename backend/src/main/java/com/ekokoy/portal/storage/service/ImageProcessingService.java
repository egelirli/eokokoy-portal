package com.ekokoy.portal.storage.service;

import com.ekokoy.portal.storage.entity.StoredFile;
import com.ekokoy.portal.storage.repository.StoredFileRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
public class ImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessingService.class);
    private static final int MAX_DIMENSION = 1920;
    private static final int THUMBNAIL_SIZE = 200;
    private static final double COMPRESS_QUALITY = 0.85;

    private final StoredFileRepository fileRepository;
    private final MinioClient minioClient;

    public ImageProcessingService(StoredFileRepository fileRepository, MinioClient minioClient) {
        this.fileRepository = fileRepository;
        this.minioClient = minioClient;
    }

    /**
     * Fotoğraf sıkıştırma ve thumbnail oluşturmayı asenkron olarak çalıştırır.
     * Orijinal dosya korunur; compressed ve thumbnail ayrı nesne olarak yüklenir.
     */
    @Async
    @Transactional
    public void processAsync(UUID fileId) {
        StoredFile file = fileRepository.findById(fileId).orElse(null);
        if (file == null || file.isDeleted()) return;

        try {
            byte[] originalBytes = downloadFromMinio(file.getBucket(), file.getObjectKey());

            // HEIC ise JPEG'e dönüştür (ImageIO eklentisi ile)
            byte[] sourceBytes = convertHeicIfNeeded(file.getMimeType(), originalBytes);

            String compressedKey = buildDerivedKey(file.getObjectKey(), "compressed");
            byte[] compressedBytes = compress(sourceBytes);
            uploadToMinio(file.getBucket(), compressedKey, compressedBytes, "image/webp");

            String thumbnailKey = buildDerivedKey(file.getObjectKey(), "thumbnail");
            byte[] thumbnailBytes = thumbnail(sourceBytes);
            uploadToMinio(file.getBucket(), thumbnailKey, thumbnailBytes, "image/webp");

            file.setCompressedKey(compressedKey);
            file.setThumbnailKey(thumbnailKey);
            fileRepository.save(file);
        } catch (Exception e) {
            log.error("Resim işleme başarısız, fileId={}: {}", fileId, e.getMessage(), e);
        }
    }

    // ── İşleme yardımcıları ──────────────────────────────────────────────────────

    private byte[] compress(byte[] source) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(source))
                .size(MAX_DIMENSION, MAX_DIMENSION)
                .keepAspectRatio(true)
                .outputFormat("webp")
                .outputQuality(COMPRESS_QUALITY)
                .toOutputStream(out);
        return out.toByteArray();
    }

    private byte[] thumbnail(byte[] source) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(source))
                .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                .crop(Positions.CENTER)
                .outputFormat("webp")
                .outputQuality(COMPRESS_QUALITY)
                .toOutputStream(out);
        return out.toByteArray();
    }

    /** HEIC / HEIF dosyaları TwelveMonkeys eklentisi yoksa JPEG'e dönüştürülmeye çalışılır. */
    private byte[] convertHeicIfNeeded(String mimeType, byte[] source) {
        if (!mimeType.equals("image/heic") && !mimeType.equals("image/heif")) return source;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(source))
                    .scale(1.0)
                    .outputFormat("jpeg")
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("HEIC → JPEG dönüşümü başarısız, orijinal baytlar kullanılıyor: {}", e.getMessage());
            return source;
        }
    }

    private byte[] downloadFromMinio(String bucket, String objectKey) throws Exception {
        try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build())) {
            return is.readAllBytes();
        }
    }

    private void uploadToMinio(String bucket, String objectKey, byte[] data, String contentType)
            throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .stream(new ByteArrayInputStream(data), data.length, -1)
                .contentType(contentType)
                .build());
    }

    private String buildDerivedKey(String originalKey, String suffix) {
        int dot = originalKey.lastIndexOf('.');
        String base = dot >= 0 ? originalKey.substring(0, dot) : originalKey;
        return base + "_" + suffix + ".webp";
    }
}

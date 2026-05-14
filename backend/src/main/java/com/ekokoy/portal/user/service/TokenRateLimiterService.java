package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token doğrulama brute force koruması.
 * Her token hash için bağımsız bir bucket tutar: 5 deneme / 15 dakika.
 */
@Service
public class TokenRateLimiterService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Token denemesine izin var mı kontrol eder; yoksa 429 fırlatır. */
    public void checkAndConsume(String tokenHash) {
        Bucket bucket = buckets.computeIfAbsent(tokenHash, k -> newBucket());
        if (!bucket.tryConsume(1)) {
            throw new EkokoyException(
                    "TOKEN_RATE_LIMIT",
                    "Çok fazla başarısız deneme. 15 dakika sonra tekrar deneyiniz.",
                    429
            );
        }
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(15))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}

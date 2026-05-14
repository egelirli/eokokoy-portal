package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login brute force koruması.
 * Her e-posta adresi için bağımsız bir bucket: 5 deneme / 15 dakika.
 */
@Service
public class LoginRateLimiterService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Belirtilen e-posta için deneme hakkı var mı kontrol eder; yoksa 429 fırlatır. */
    public void checkAndConsume(String email) {
        String key = email.toLowerCase();
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());
        if (!bucket.tryConsume(1)) {
            throw new EkokoyException(
                    "LOGIN_RATE_LIMIT",
                    "Çok fazla başarısız giriş denemesi. 15 dakika sonra tekrar deneyiniz.",
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

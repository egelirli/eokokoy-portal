package com.ekokoy.portal.user.service;

import com.ekokoy.portal.common.EkokoyException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token doğrulama brute force koruması.
 * Yalnızca başarısız (token bulunamayan) denemeler sayılır: 5 başarısız/15 dakika.
 */
@Service
public class TokenRateLimiterService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Bucket doluysa 429 fırlatır; dolmamışsa geçirir (token tüketmez).
     * Doğrulama girişiminden önce çağrılır.
     */
    public void assertNotBlocked(String tokenHash) {
        Bucket bucket = buckets.get(tokenHash);
        if (bucket != null && bucket.getAvailableTokens() == 0) {
            throw new EkokoyException(
                    "TOKEN_RATE_LIMIT",
                    "Çok fazla başarısız deneme. 15 dakika sonra tekrar deneyiniz.",
                    429
            );
        }
    }

    /**
     * Başarısız denemede çağrılır; bucket'tan 1 token tüketir.
     * 5. başarısız denemeden sonra bucket boşalır ve assertNotBlocked 429 döner.
     */
    public void recordFailedAttempt(String tokenHash) {
        Bucket bucket = buckets.computeIfAbsent(tokenHash, k -> newBucket());
        bucket.tryConsume(1);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(15))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}

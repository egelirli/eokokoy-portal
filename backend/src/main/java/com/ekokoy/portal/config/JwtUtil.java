package com.ekokoy.portal.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final JwtProperties props;

    public JwtUtil(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JWT access token üretir. Payload'a roles, permissions ve property_ids eklenir.
     */
    public String generateAccessToken(UUID userId, List<String> roles, List<String> permissions, List<UUID> propertyIds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + props.getAccessExpiration() * 1000L);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("property_ids", propertyIds.stream().map(UUID::toString).toList())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Token doğrular; geçersizse null döner.
     */
    public Claims validateAndExtract(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        return (List<String>) claims.get("roles", List.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(Claims claims) {
        return (List<String>) claims.get("permissions", List.class);
    }
}

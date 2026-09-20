package com.example.pipelineservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * FIXES:
 *
 * BUG 1 — SignatureException
 *   CAUSE: This class used secret="devsecops-stb-jwt-secret-key-minimum-32-chars"
 *          while auth-service used a Base64 string as its secret.
 *          Different secret strings → different key bytes → signatures never match.
 *   FIX  : Both services now use the SAME plain UTF-8 string from application.properties.
 *          Key construction is identical: Keys.hmacShaKeyFor(secret.getBytes(UTF_8))
 *
 * BUG 2 — Expired token causes null return, then NullPointerException downstream
 *   CAUSE: ExpiredJwtException was caught silently, returning null.
 *          JwtAuthFilter then called null.getSubject() → NPE.
 *   FIX  : ExpiredJwtException caught separately, logs a clear message with
 *          expiry time, returns null safely. JwtAuthFilter already handles null.
 */
@Component
public class JwtTokenParser {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // FIX: MUST be identical to auth-service JwtService.init()
        // Plain UTF-8 bytes — no Base64 decoding
        byte[] keyBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters. Current: " + keyBytes.length);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Parse and verify a JWT token.
     * Returns null if token is invalid, expired, or malformed.
     * The caller (JwtAuthFilter) treats null as "not authenticated".
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .clockSkewSeconds(120) // allow 2 min clock drift between services
                    .build()
                    .parseSignedClaims(clean(token))
                    .getPayload();

        } catch (ExpiredJwtException e) {
            // FIX: log clearly so developers can diagnose expired token issues
            // Return null so JwtAuthFilter returns 401 → frontend calls /auth/refresh
            System.out.println("❌ JWT expired: " + e.getMessage());
            return null;

        } catch (JwtException e) {
            // Wrong signature, malformed token, unsupported algorithm, etc.
            System.out.println("❌ JWT invalid: " + e.getMessage());
            return null;

        } catch (IllegalArgumentException e) {
            System.out.println("❌ JWT empty or null: " + e.getMessage());
            return null;
        }
    }

    private String clean(String token) {
        return token != null ? token.replace("Bearer ", "").trim() : "";
    }
}
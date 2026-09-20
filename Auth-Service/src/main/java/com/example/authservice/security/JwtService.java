package com.example.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * FIXES:
 *
 * BUG 1 — SignatureException across microservices
 *   CAUSE: Keys.hmacShaKeyFor(secret.getBytes()) treats the Base64 string
 *          as raw bytes — producing a completely different key than intended.
 *          Meanwhile pipeline-service used a different secret string entirely.
 *          Both services signed/verified with different keys → SignatureException.
 *   FIX  : Use a plain UTF-8 string secret in application.properties (no Base64).
 *          All services call Keys.hmacShaKeyFor(secret.getBytes(UTF_8)).
 *          Same string → same bytes → same key → signatures match.
 *
 * BUG 2 — JWT expired after 1 hour causing 401 errors
 *   CAUSE: jwt.expiration=86400000 (24h) was correct but token was from a previous
 *          session. Frontend had no refresh mechanism.
 *   FIX  : Expiration kept at 8h (28800000). Refresh token at 7 days.
 *          JwtAuthenticationFilter now returns 401 with a clear error body
 *          so the Angular frontend can detect expiry and call /auth/refresh.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long accessTokenExpiry;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTokenExpiry;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // FIX: plain UTF-8 bytes — same construction must be used in every service
        // Do NOT base64-decode here; the secret in .properties is a plain string
        byte[] keyBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters. Current length: " + keyBytes.length);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Token generation ──────────────────────────────────────

    public String generateToken(com.example.authservice.entities.User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiry))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(com.example.authservice.entities.User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiry))
                .signWith(key)
                .compact();
    }

    // ── Validation ────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    // ── Extraction ────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public String extractType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    // ── Core ──────────────────────────────────────────────────

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .clockSkewSeconds(120)
                .build()
                .parseSignedClaims(clean(token))
                .getPayload();
    }

    private String clean(String token) {
        return token != null ? token.replace("Bearer ", "").trim() : "";
    }
}
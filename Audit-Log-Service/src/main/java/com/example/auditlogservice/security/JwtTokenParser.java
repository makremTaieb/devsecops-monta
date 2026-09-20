package com.example.auditlogservice.security;

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

@Component
public class JwtTokenParser {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .clockSkewSeconds(120)
                    .build()
                    .parseSignedClaims(clean(token))
                    .getPayload();
        } catch (ExpiredJwtException e) {
            System.out.println("❌ JWT expired: " + e.getMessage());
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("❌ JWT invalid: " + e.getMessage());
            return null;
        }
    }

    private String clean(String token) {
        return token != null ? token.replace("Bearer ", "").trim() : "";
    }
}

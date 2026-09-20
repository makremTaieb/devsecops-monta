package com.example.authservice.service;

import com.example.authservice.Dto.*;
import com.example.authservice.client.AuditLogClient;
import com.example.authservice.entities.User;
import com.example.authservice.exception.ResourceNotFoundException;
import com.example.authservice.repositories.UserRepository;
import com.example.authservice.security.JwtService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService      jwtService;
    private final AuditLogClient  auditLogClient;      // ← NEW

    // ================= REGISTER =================
    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("Username already taken");

        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email already registered");

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // ── AUDIT ──
        auditLogClient.send(Map.of(
                "username",      user.getUsername(),
                "userId",        user.getId(),
                "action",        "USER_REGISTER",
                "resource",      "USER",
                "resourceId",    user.getId(),
                "details",       "New user registered with role " + user.getRole(),
                "status",        "SUCCESS",
                "sourceService", "auth-service"
        ));

        return buildAuthResponse(user);
    }

    // ================= LOGIN =================
    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // ── AUDIT failed login ──
            auditLogClient.send(Map.of(
                    "username",      request.getUsername(),
                    "action",        "USER_LOGIN",
                    "resource",      "USER",
                    "details",       "Failed login attempt — invalid password",
                    "status",        "FAILURE",
                    "sourceService", "auth-service"
            ));
            throw new RuntimeException("Invalid credentials");
        }

        // ── AUDIT successful login ──
        auditLogClient.send(Map.of(
                "username",      user.getUsername(),
                "userId",        user.getId(),
                "action",        "USER_LOGIN",
                "resource",      "USER",
                "resourceId",    user.getId(),
                "details",       "User logged in",
                "status",        "SUCCESS",
                "sourceService", "auth-service"
        ));

        return buildAuthResponse(user);
    }

    // ================= REFRESH TOKEN =================
    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();

        String type = jwtService.extractType(refreshToken);
        if (!"refresh".equals(type))
            throw new RuntimeException("Invalid token type");

        String username = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (jwtService.isTokenExpired(refreshToken))
            throw new RuntimeException("Refresh token expired");

        // ── AUDIT ──
        auditLogClient.send(Map.of(
                "username",      user.getUsername(),
                "userId",        user.getId(),
                "action",        "USER_REFRESH_TOKEN",
                "resource",      "USER",
                "resourceId",    user.getId(),
                "details",       "Access token refreshed",
                "status",        "SUCCESS",
                "sourceService", "auth-service"
        ));

        return buildAuthResponse(user);
    }

    // ================= GET CURRENT USER =================
    @Override
    public UserResponse getCurrentUser(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // ================= HELPER =================
    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}

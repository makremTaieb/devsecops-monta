// ---------- AuthController.java ----------
// FIX: added @Valid on all request bodies to trigger validation annotations
// FIX: added /refresh endpoint — was missing despite the DTO existing
// FIX: added /me endpoint — returns current user from JWT
package com.example.authservice.controller;


import com.example.authservice.Dto.*;
import com.example.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // FIX: refresh endpoint was completely absent
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    // NEW: returns the currently authenticated user's profile
    @GetMapping("/me")
    public UserResponse getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        return authService.getCurrentUser(userDetails.getUsername());
    }

    @GetMapping("/test")
    public String test() {
        return "AUTH OK";
    }
}
 
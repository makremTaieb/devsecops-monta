// ---------- AuthService.java ----------
// FIX: added refreshToken() and getCurrentUser() — were missing
package com.example.authservice.service;


import com.example.authservice.Dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    // FIX: refresh token flow — DTO existed but method was never added
    AuthResponse refreshToken(RefreshTokenRequest request);

    // NEW: for the /me endpoint (dashboard, chatbot "who am I")
    UserResponse getCurrentUser(String username);
}
 
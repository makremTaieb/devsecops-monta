package com.example.authservice.Dto;// ---------- AuthResponse.java ----------
// FIX: was only returning 'token' — now returns accessToken + refreshToken + role
// RefreshTokenRequest DTO existed but refresh flow was never implemented

import com.example.authservice.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    private String username;

    private Role role;
}
 
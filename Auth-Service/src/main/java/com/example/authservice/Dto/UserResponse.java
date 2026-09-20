package com.example.authservice.Dto;// ---------- UserResponse.java (NEW — for /me endpoint) ----------

import com.example.authservice.entities.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
}
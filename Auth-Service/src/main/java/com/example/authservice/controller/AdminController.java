package com.example.authservice.controller;

import com.example.authservice.Dto.UserResponse;
import com.example.authservice.entities.Role;
import com.example.authservice.entities.User;
import com.example.authservice.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Admin-only endpoints for user management.
 * All routes require ADMIN role.
 */
@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;

    /** List all users */
    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /** Get user by ID */
    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /** Enable or disable a user */
    @PutMapping("/users/{id}/enabled")
    public UserResponse setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setEnabled(enabled);
        return toResponse(userRepository.save(user));
    }

    /** Change a user's role */
    @PutMapping("/users/{id}/role")
    public UserResponse changeRole(@PathVariable Long id, @RequestParam Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    /** Delete a user */
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .role(u.getRole()).enabled(u.isEnabled())
                .createdAt(u.getCreatedAt())
                .build();
    }
}

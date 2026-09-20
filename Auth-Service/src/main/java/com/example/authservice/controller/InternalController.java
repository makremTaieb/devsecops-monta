package com.example.authservice.controller;

import com.example.authservice.entities.Role;
import com.example.authservice.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal service-to-service endpoints — no JWT required.
 * Only accessible from within the Kubernetes cluster (not exposed via Gateway).
 */
@RestController
@RequestMapping("/api/auth/internal")
@RequiredArgsConstructor
public class InternalController {

    private final UserRepository userRepository;

    /** Returns the email of every ADMIN user — used by Notification-Service */
    @GetMapping("/admin-emails")
    public List<String> getAdminEmails() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.isEnabled())
                .map(u -> u.getEmail())
                .filter(e -> e != null && !e.isBlank())
                .toList();
    }
}

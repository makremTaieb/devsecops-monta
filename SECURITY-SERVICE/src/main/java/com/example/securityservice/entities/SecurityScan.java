package com.example.securityservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "security_scan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;

    private Long executionId;

    private Double score;

    private Boolean blocked;

    private LocalDateTime createdAt;

    // FIX: initialize to avoid NullPointerException when adding vulnerabilities
    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vulnerability> vulnerabilities = new ArrayList<>();
}
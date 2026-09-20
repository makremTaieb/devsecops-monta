// ---------- SecurityScanRepository.java ----------
package com.example.securityservice.repository;

import com.example.securityservice.entities.SecurityScan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SecurityScanRepository extends JpaRepository<SecurityScan, Long> {

    List<SecurityScan> findByProjectId(Long projectId);

    List<SecurityScan> findByExecutionId(Long executionId);

    // Latest scan for a given execution (used by policy gate)
    Optional<SecurityScan> findTopByExecutionIdOrderByCreatedAtDesc(Long executionId);
}
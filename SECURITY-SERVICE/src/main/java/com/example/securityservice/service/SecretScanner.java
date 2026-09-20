package com.example.securityservice.service;

import com.example.securityservice.entities.Vulnerability;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Secret scanner — results come from Gitleaks (parsed by GitleaksParser).
 * This class exists for the ScanOrchestrator but does nothing extra.
 */
@Service
public class SecretScanner {
    public List<Vulnerability> scan(Long projectId) {
        // Real secret detection results are provided by GitleaksParser via /scan endpoint.
        return List.of();
    }
}

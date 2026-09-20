package com.example.securityservice.service;

import com.example.securityservice.entities.Vulnerability;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SCA scanner — results come from Trivy (parsed by TrivyParser).
 * This class exists for the ScanOrchestrator but does nothing extra.
 */
@Service
public class ScaScanner {
    public List<Vulnerability> scan(Long projectId) {
        // Real SCA results are provided by TrivyParser via the /scan endpoint.
        return List.of();
    }
}

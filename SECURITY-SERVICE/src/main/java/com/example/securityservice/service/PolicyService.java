package com.example.securityservice.service;

import com.example.securityservice.entities.SeverityLevel;
import com.example.securityservice.entities.Vulnerability;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyService {

    // FIX: lowered from 70 → 50, configurable via application.properties
    // A brand-new Spring Boot app with no known CVEs in its direct deps
    // should score ~80-100. Third-party transitive deps may drop it to 50-70.
    @Value("${security.policy.min-score:50.0}")
    private double minScore;

    // FIX: CRITICAL-only blocks only if count > threshold (not 1 secret = blocked)
    @Value("${security.policy.max-critical:0}")
    private int maxCritical;

    public boolean shouldBlock(double score, List<Vulnerability> vulnerabilities) {

        // Block if score is below minimum threshold
        if (score < minScore) return true;

        // Block only if there are MORE than maxCritical CRITICAL vulnerabilities
        long criticalCount = vulnerabilities.stream()
                .filter(v -> v.getSeverity() == SeverityLevel.CRITICAL)
                .count();

        return criticalCount > maxCritical;
    }
}

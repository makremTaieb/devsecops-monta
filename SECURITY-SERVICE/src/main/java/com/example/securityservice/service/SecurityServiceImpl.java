package com.example.securityservice.service;

import com.example.securityservice.dto.ScanDetailResponse;
import com.example.securityservice.dto.SecurityScanResponse;
import com.example.securityservice.entities.SecurityScan;
import com.example.securityservice.entities.SeverityLevel;
import com.example.securityservice.entities.Vulnerability;
import com.example.securityservice.mapper.SecurityMapper;
import com.example.securityservice.repository.SecurityScanRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SecurityServiceImpl implements SecurityService {

    private final SecurityScanRepository scanRepository;
    private final SecurityMapper         securityMapper;
    private final TrivyParser            trivyParser;
    private final GitleaksParser         gitleaksParser;
    private final PolicyService          policyService;

    // ── SCAN ─────────────────────────────────────────────────────────────────
    @Override
    public SecurityScanResponse scan(
            Long executionId,
            Long projectId,
            MultipartFile trivy,
            MultipartFile gitleaks) {

        List<Vulnerability> vulnerabilities = new ArrayList<>();
        vulnerabilities.addAll(trivyParser.parse(trivy));
        vulnerabilities.addAll(gitleaksParser.parse(gitleaks));

        double  score   = calculateScore(vulnerabilities);
        boolean blocked = policyService.shouldBlock(score, vulnerabilities);

        SecurityScan scan = SecurityScan.builder()
                .executionId(executionId)
                .projectId(projectId)
                .score(score)
                .blocked(blocked)
                .createdAt(LocalDateTime.now())
                .build();

        vulnerabilities.forEach(v -> v.setScan(scan));
        scan.setVulnerabilities(vulnerabilities);

        SecurityScan saved = scanRepository.save(scan);

        System.out.printf("[SecurityService] executionId=%d projectId=%d score=%.1f blocked=%b vulns=%d%n",
                executionId, projectId, score, blocked, vulnerabilities.size());

        return securityMapper.toResponse(saved);
    }

    // ── GET BY EXECUTION ─────────────────────────────────────────────────────
    @Override
    public ScanDetailResponse getScanByExecution(Long executionId) {
        SecurityScan scan = scanRepository
                .findTopByExecutionIdOrderByCreatedAtDesc(executionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No scan for execution: " + executionId));
        return securityMapper.toDetailResponse(scan);
    }

    // ── GET BY PROJECT ───────────────────────────────────────────────────────
    @Override
    public List<ScanDetailResponse> getScansByProject(Long projectId) {
        return scanRepository.findByProjectId(projectId)
                .stream()
                .map(securityMapper::toDetailResponse)
                .toList();
    }

    // ── SCORE CALCULATION ────────────────────────────────────────────────────
    // FIX: cap deduction per severity category so a project with many LOW vulns
    // doesn't score 0. Real-world scoring: group by severity, cap each group.
    private double calculateScore(List<Vulnerability> vulnerabilities) {

        if (vulnerabilities.isEmpty()) return 100.0;

        // Group by severity
        Map<SeverityLevel, Long> counts = vulnerabilities.stream()
                .filter(v -> v.getSeverity() != null)
                .collect(Collectors.groupingBy(Vulnerability::getSeverity, Collectors.counting()));

        double score = 100.0;

        // Deduct per category — capped so one category can't drain score to 0
        // CRITICAL: -20 per vuln, max -40
        long critical = counts.getOrDefault(SeverityLevel.CRITICAL, 0L);
        score -= Math.min(critical * 20, 40);

        // HIGH: -10 per vuln, max -30
        long high = counts.getOrDefault(SeverityLevel.HIGH, 0L);
        score -= Math.min(high * 10, 30);

        // MEDIUM: -5 per vuln, max -20
        long medium = counts.getOrDefault(SeverityLevel.MEDIUM, 0L);
        score -= Math.min(medium * 5, 20);

        // LOW: -2 per vuln, max -10
        long low = counts.getOrDefault(SeverityLevel.LOW, 0L);
        score -= Math.min(low * 2, 10);

        return Math.max(score, 0.0);
    }
}

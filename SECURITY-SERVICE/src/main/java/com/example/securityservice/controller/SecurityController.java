// ---------- SecurityController.java ----------
package com.example.securityservice.controller;

import com.example.securityservice.dto.ScanDetailResponse;
import com.example.securityservice.dto.SecurityScanResponse;
import com.example.securityservice.service.SecurityService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;

    // =========================
    // MAIN SCAN (from Jenkins)
    // =========================
    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SecurityScanResponse scan(
            @RequestParam Long executionId,
            @RequestParam Long projectId,
            @RequestParam MultipartFile trivy,
            @RequestParam MultipartFile gitleaks
    ) {
        return securityService.scan(executionId, projectId, trivy, gitleaks);
    }

    // =========================
    // GET SCAN BY EXECUTION
    // =========================
    @GetMapping("/scan/execution/{executionId}")
    public ScanDetailResponse getScanByExecution(@PathVariable Long executionId) {
        return securityService.getScanByExecution(executionId);
    }

    // =========================
    // GET SCANS BY PROJECT
    // =========================
    @GetMapping("/scan/project/{projectId}")
    public List<ScanDetailResponse> getScansByProject(@PathVariable Long projectId) {
        return securityService.getScansByProject(projectId);
    }
}
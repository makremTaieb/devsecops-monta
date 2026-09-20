// ---------- SecurityService.java ----------
package com.example.securityservice.service;

import com.example.securityservice.dto.ScanDetailResponse;
import com.example.securityservice.dto.SecurityScanResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SecurityService {

    SecurityScanResponse scan(
            Long executionId,
            Long projectId,
            MultipartFile trivy,
            MultipartFile gitleaks
    );

    ScanDetailResponse getScanByExecution(Long executionId);

    List<ScanDetailResponse> getScansByProject(Long projectId);
}
// ---------- SecurityMapper.java ----------
package com.example.securityservice.mapper;

import com.example.securityservice.dto.ScanDetailResponse;
import com.example.securityservice.dto.SecurityScanResponse;
import com.example.securityservice.dto.VulnerabilityResponse;
import com.example.securityservice.entities.SecurityScan;
import com.example.securityservice.entities.Vulnerability;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SecurityMapper {

    // Used by the scan endpoint — minimal response for the pipeline-service
    public SecurityScanResponse toResponse(SecurityScan scan) {
        return SecurityScanResponse.builder()
                .scanId(scan.getId())
                .securityScore(scan.getScore())
                .blocked(scan.getBlocked())
                .build();
    }

    // Used by the detail endpoint — full response with vulnerabilities list
    public ScanDetailResponse toDetailResponse(SecurityScan scan) {
        List<VulnerabilityResponse> vulnResponses = scan.getVulnerabilities()
                .stream()
                .map(this::toVulnResponse)
                .toList();

        return ScanDetailResponse.builder()
                .scanId(scan.getId())
                .projectId(scan.getProjectId())
                .executionId(scan.getExecutionId())
                .securityScore(scan.getScore())
                .blocked(scan.getBlocked())
                .createdAt(scan.getCreatedAt())
                .vulnerabilities(vulnResponses)
                .build();
    }

    private VulnerabilityResponse toVulnResponse(Vulnerability v) {
        return VulnerabilityResponse.builder()
                .id(v.getId())
                .type(v.getType())
                .severity(v.getSeverity())
                .description(v.getDescription())
                .filePath(v.getFilePath())
                .cve(v.getCve())
                .build();
    }
}
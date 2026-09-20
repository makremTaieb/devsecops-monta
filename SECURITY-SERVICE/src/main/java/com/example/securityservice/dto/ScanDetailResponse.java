// ---------- ScanDetailResponse.java (NEW — for the detail endpoint) ----------
package com.example.securityservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanDetailResponse {

    private Long scanId;
    private Long projectId;
    private Long executionId;
    private Double securityScore;
    private Boolean blocked;
    private LocalDateTime createdAt;
    private List<VulnerabilityResponse> vulnerabilities;
}
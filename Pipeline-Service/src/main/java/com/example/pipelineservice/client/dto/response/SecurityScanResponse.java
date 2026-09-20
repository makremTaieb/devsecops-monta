// ---------- SecurityScanResponse.java ----------
// FIX: the original had a broken "SecurityScanRequest" in this folder doing double duty
// Now this is a proper response DTO that matches what security-service returns
package com.example.pipelineservice.client.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecurityScanResponse {
    private Long scanId;
    private Double securityScore;
    private Boolean blocked;
}
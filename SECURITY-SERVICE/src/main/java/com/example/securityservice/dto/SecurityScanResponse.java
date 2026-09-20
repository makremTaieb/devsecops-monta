package com.example.securityservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SecurityScanResponse {

    private Long scanId;
    private Double securityScore;
    private Boolean blocked;
}
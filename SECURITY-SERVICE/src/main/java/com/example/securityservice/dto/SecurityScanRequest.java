package com.example.securityservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecurityScanRequest {

    private Long projectId;
    private Long executionId;
}
package com.example.pipelineservice.client.dto.request;

import lombok.Getter;
import lombok.Setter;

// Ce que le pipeline-service ENVOIE au security-service
// "Scanne ce projet pour cette exécution"
@Getter
@Setter
public class SecurityScanRequest {

    private Long projectId;

    private Long executionId;
}
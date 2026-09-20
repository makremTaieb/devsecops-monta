package com.example.pipelineservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Fire-and-forget client that posts audit events to the Audit-Log-Service.
 * Failures are logged but never propagate — auditing must never break pipeline ops.
 */
@Component
@Slf4j
public class AuditLogClient {

    private final RestTemplate restTemplate;
    private final String auditUrl;

    public AuditLogClient(
            RestTemplate restTemplate,
            @Value("${audit.service.url:http://audit-log-service.apps.svc.cluster.local:8085/api/audit/log}")
            String auditUrl) {
        this.restTemplate = restTemplate;
        this.auditUrl = auditUrl;
    }

    @Async
    public void send(Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(auditUrl, entity, Void.class);
        } catch (Exception e) {
            log.warn("⚠️  Audit log delivery failed (non-critical): {}", e.getMessage());
        }
    }
}

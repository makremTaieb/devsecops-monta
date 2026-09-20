package com.example.pipelineservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Fire-and-forget client that posts pipeline events to the Notification-Service.
 * Failures are logged but never propagate — notifications must never break pipeline ops.
 */
@Component
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final String notificationUrl;

    public NotificationClient(
            RestTemplate restTemplate,
            @Value("${notification.service.url:http://notification-service.apps.svc.cluster.local:8087/api/notifications/pipeline-event}")
            String notificationUrl) {
        this.restTemplate = restTemplate;
        this.notificationUrl = notificationUrl;
    }

    @Async
    public void sendPipelineEvent(Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(notificationUrl, entity, Void.class);
            log.info("✅ Notification sent for pipeline event: {}", payload.get("eventType"));
        } catch (Exception e) {
            log.warn("⚠️  Notification delivery failed (non-critical): {}", e.getMessage());
        }
    }
}

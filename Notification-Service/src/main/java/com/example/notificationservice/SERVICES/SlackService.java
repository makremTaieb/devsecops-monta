package com.example.notificationservice.SERVICES;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackService {

    @Value("${notification.slack.enabled:false}")
    private boolean enabled;

    @Value("${notification.slack.webhook-url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate;

    public void send(String message) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Slack disabled — skipping notification");
            return;
        }
        try {
            String payload = "{\"text\": \"" +
                    message.replace("\"", "\\\"") + "\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.exchange(webhookUrl, HttpMethod.POST,
                    new HttpEntity<>(payload, headers), String.class);
            log.info("Slack notification sent");
        } catch (Exception e) {
            log.error("Slack send failed: {}", e.getMessage());
            throw new RuntimeException("Slack send failed: " + e.getMessage());
        }
    }
}
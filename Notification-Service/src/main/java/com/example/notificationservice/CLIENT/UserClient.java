package com.example.notificationservice.CLIENT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {

    private final RestTemplate restTemplate;

    @Value("${services.auth-url:http://auth-service.apps.svc.cluster.local:8081}")
    private String authServiceUrl;

    /**
     * Fetches emails of all enabled ADMIN users from Auth-Service internal endpoint.
     * Falls back to empty list on any error.
     */
    public List<String> getAdminEmails() {
        try {
            String url = authServiceUrl + "/api/auth/internal/admin-emails";
            List<String> emails = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<String>>() {}
            ).getBody();
            return (emails != null && !emails.isEmpty()) ? emails : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Could not fetch admin emails from Auth-Service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}

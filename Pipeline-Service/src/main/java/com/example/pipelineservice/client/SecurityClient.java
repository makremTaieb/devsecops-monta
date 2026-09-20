package com.example.pipelineservice.client;

import com.example.pipelineservice.client.dto.request.SecurityScanRequest;
import com.example.pipelineservice.client.dto.response.SecurityScanResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "security-service")
public interface SecurityClient {

    // Envoie un REQUEST, reçoit un RESPONSE — logique !
    @PostMapping("/api/security/scan")
    SecurityScanResponse scan(@RequestBody SecurityScanRequest request);
}
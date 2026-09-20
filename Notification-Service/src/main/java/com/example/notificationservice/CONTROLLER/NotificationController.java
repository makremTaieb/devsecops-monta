package com.example.notificationservice.CONTROLLER;

import com.example.notificationservice.DTOs.NotificationResponse;
import com.example.notificationservice.DTOs.PipelineEventRequest;
import com.example.notificationservice.DTOs.SecurityAlertRequest;
import com.example.notificationservice.SERVICES.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Called by pipeline-service when execution finishes
    @PostMapping("/pipeline-event")
    public ResponseEntity<NotificationResponse> pipelineEvent(
            @Valid @RequestBody PipelineEventRequest request) {
        return ResponseEntity.ok(
                notificationService.handlePipelineEvent(request));
    }

    // Called by security-service when scan finds issues
    @PostMapping("/security-alert")
    public ResponseEntity<NotificationResponse> securityAlert(
            @Valid @RequestBody SecurityAlertRequest request) {
        return ResponseEntity.ok(
                notificationService.handleSecurityAlert(request));
    }

    // Get all notifications (Angular dashboard)
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    // Get by source service
    @GetMapping("/source/{service}")
    public ResponseEntity<List<NotificationResponse>> getBySource(
            @PathVariable String service) {
        return ResponseEntity.ok(
                notificationService.getBySourceService(service));
    }
}


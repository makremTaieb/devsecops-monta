package com.example.notificationservice.SERVICES;


import com.example.notificationservice.DTOs.NotificationResponse;
import com.example.notificationservice.DTOs.PipelineEventRequest;
import com.example.notificationservice.DTOs.SecurityAlertRequest;
import com.example.notificationservice.entities.*;
import com.example.notificationservice.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService           emailService;
    private final SlackService           slackService;

    @Value("${notification.email.default-recipient:devops@devsecops.local}")
    private String defaultRecipient;

    @Override
    public NotificationResponse handlePipelineEvent(PipelineEventRequest req) {
        String subject = "[DevSecOps] Pipeline "
                + (req.getEventType() == EventType.PIPELINE_SUCCESS ? "SUCCESS" : "FAILED")
                + " — " + req.getProjectName();

        String message = String.format(
                "Project: %s\nBranch: %s\nCommit: %s\nTriggered by: %s\nStatus: %s",
                req.getProjectName(), req.getBranch(),
                req.getCommitHash(), req.getTriggeredBy(),
                req.getEventType().name());

        Notification n = Notification.builder()
                .eventType(req.getEventType())
                .channel(NotificationChannel.EMAIL)
                .recipient(defaultRecipient)
                .subject(subject)
                .message(message)
                .pipelineExecutionId(req.getPipelineExecutionId())
                .projectId(req.getProjectId())
                .sourceService("pipeline-service")
                .build();

        n = sendEmail(n);

        try { slackService.send("[" + req.getEventType() + "] " + subject); }
        catch (Exception e) { log.warn("Slack failed (non-blocking): {}", e.getMessage()); }

        return toResponse(notificationRepository.save(n));
    }

    @Override
    public NotificationResponse handleSecurityAlert(SecurityAlertRequest req) {
        String subject = "[DevSecOps] Security Alert — "
                + req.getEventType().name() + " — " + req.getProjectName();

        String message = String.format(
                "Project: %s\nScore: %.1f\nCritical: %d | High: %d\nStatus: %s",
                req.getProjectName(),
                req.getSecurityScore() != null ? req.getSecurityScore() : 0.0,
                req.getCriticalCount() != null ? req.getCriticalCount() : 0,
                req.getHighCount()    != null ? req.getHighCount()    : 0,
                req.getEventType().name());

        Notification n = Notification.builder()
                .eventType(req.getEventType())
                .channel(NotificationChannel.EMAIL)
                .recipient(defaultRecipient)
                .subject(subject)
                .message(message)
                .projectId(req.getProjectId())
                .pipelineExecutionId(req.getExecutionId())
                .sourceService("security-service")
                .build();

        n = sendEmail(n);

        try { slackService.send("[SECURITY] " + subject); }
        catch (Exception e) { log.warn("Slack alert failed: {}", e.getMessage()); }

        return toResponse(notificationRepository.save(n));
    }

    @Override
    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll().stream()
                .map(this::toResponse).toList();
    }

    @Override
    public List<NotificationResponse> getBySourceService(String src) {
        return notificationRepository.findBySourceService(src).stream()
                .map(this::toResponse).toList();
    }

    private Notification sendEmail(Notification n) {
        try {
            emailService.send(n.getRecipient(), n.getSubject(), n.getMessage());
            n.setStatus(NotificationStatus.SENT);
            n.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            n.setStatus(NotificationStatus.FAILED);
            n.setErrorMessage(e.getMessage());
        }
        return n;
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .eventType(n.getEventType())
                .channel(n.getChannel())
                .status(n.getStatus())
                .recipient(n.getRecipient())
                .subject(n.getSubject())
                .message(n.getMessage())
                .projectId(n.getProjectId())
                .pipelineExecutionId(n.getPipelineExecutionId())
                .createdAt(n.getCreatedAt())
                .sentAt(n.getSentAt())
                .build();
    }
}

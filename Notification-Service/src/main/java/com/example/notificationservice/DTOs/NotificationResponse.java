package com.example.notificationservice.DTOs;

import com.example.notificationservice.entities.*;
import lombok.*;

import java.time.LocalDateTime;

@Data @Builder
public class NotificationResponse {
    private Long                id;
    private EventType           eventType;
    private NotificationChannel channel;
    private NotificationStatus  status;
    private String              recipient;
    private String              subject;       // ✅ FIXED: was missing
    private String              message;
    private Long                projectId;
    private Long                pipelineExecutionId;
    private LocalDateTime       createdAt;
    private LocalDateTime       sentAt;
}

package com.example.notificationservice.SERVICES;



import com.example.notificationservice.DTOs.NotificationResponse;
import com.example.notificationservice.DTOs.PipelineEventRequest;
import com.example.notificationservice.DTOs.SecurityAlertRequest;

import java.util.List;

public interface NotificationService {
    NotificationResponse handlePipelineEvent(PipelineEventRequest request);
    NotificationResponse handleSecurityAlert(SecurityAlertRequest request);
    List<NotificationResponse> getAll();
    List<NotificationResponse> getBySourceService(String sourceService);
}

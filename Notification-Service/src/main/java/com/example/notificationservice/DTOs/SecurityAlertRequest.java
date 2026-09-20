package com.example.notificationservice.DTOs;

import com.example.notificationservice.entities.EventType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SecurityAlertRequest {
    @NotNull private Long      projectId;
    @NotNull private Long      executionId;
    @NotNull private EventType eventType;
    private Double  securityScore;
    private Integer criticalCount;
    private Integer highCount;
    private String  projectName;
}

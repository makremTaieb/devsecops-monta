package com.example.auditlogservice.dto;

import com.example.auditlogservice.entities.ActionStatus;
import com.example.auditlogservice.entities.ActionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditLogRequest {

    private Long   userId;
    private String username;

    @NotNull
    private ActionType action;

    private String resource;
    private Long   resourceId;
    private String details;
    private String ipAddress;

    @NotNull
    private ActionStatus status;

    private String sourceService;
}

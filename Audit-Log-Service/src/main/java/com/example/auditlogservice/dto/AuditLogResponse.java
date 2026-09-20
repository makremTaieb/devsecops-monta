package com.example.auditlogservice.dto;

import com.example.auditlogservice.entities.ActionStatus;
import com.example.auditlogservice.entities.ActionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long          id;
    private LocalDateTime timestamp;
    private Long          userId;
    private String        username;
    private ActionType    action;
    private String        resource;
    private Long          resourceId;
    private String        details;
    private String        ipAddress;
    private ActionStatus  status;
    private String        sourceService;
}

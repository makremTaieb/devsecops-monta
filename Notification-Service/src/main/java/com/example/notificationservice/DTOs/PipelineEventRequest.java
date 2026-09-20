package com.example.notificationservice.DTOs;

import com.example.notificationservice.entities.EventType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PipelineEventRequest {
    @NotNull private Long      pipelineExecutionId;
    @NotNull private Long      projectId;
    @NotNull private EventType eventType;
    private String projectName;
    private String branch;
    private String commitHash;
    private String triggeredBy;
}
package com.example.pipelineservice.client.dto.response;

import com.example.pipelineservice.entities.PipelineStatus;
import com.example.pipelineservice.entities.StageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StageExecutionResponse {

    private Long stageId;
    private String stageName;
    private StageType stageType;
    private PipelineStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
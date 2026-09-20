package com.example.pipelineservice.client.dto.response;

import com.example.pipelineservice.entities.PipelineStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PipelineResponse {
    private Long           id;
    private String         name;
    private String         jenkinsJobName;
    private Long           projectId;
    private PipelineStatus status;
    private LocalDateTime  createdAt;
}
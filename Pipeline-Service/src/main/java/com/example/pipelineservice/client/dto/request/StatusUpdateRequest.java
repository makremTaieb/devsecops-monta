package com.example.pipelineservice.client.dto.request;

import com.example.pipelineservice.entities.PipelineStatus;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    private PipelineStatus status;
}

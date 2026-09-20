package com.example.pipelineservice.service;

import com.example.pipelineservice.client.dto.request.ExecutionRequest;
import com.example.pipelineservice.client.dto.response.ExecutionResponse;
import com.example.pipelineservice.entities.PipelineStatus;

import java.util.List;

public interface ExecutionService {

    ExecutionResponse executePipeline(Long pipelineId, ExecutionRequest request);

    List<ExecutionResponse> getExecutionsByPipeline(Long pipelineId);

    ExecutionResponse getExecutionById(Long executionId);

    void updateStatus(Long executionId, PipelineStatus status);
}
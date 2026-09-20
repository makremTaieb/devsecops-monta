package com.example.pipelineservice.service;

import com.example.pipelineservice.client.dto.request.CreatePipelineRequest;
import com.example.pipelineservice.client.dto.response.PipelineResponse;
import java.util.List;

public interface PipelineService {
    PipelineResponse createPipeline(Long projectId, CreatePipelineRequest request);
    List<PipelineResponse> getPipelinesByProject(Long projectId);
    PipelineResponse getPipelineById(Long pipelineId);
    PipelineResponse updatePipeline(Long pipelineId, CreatePipelineRequest request);
    void deletePipeline(Long pipelineId);
}
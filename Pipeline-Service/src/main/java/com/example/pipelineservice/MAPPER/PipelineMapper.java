package com.example.pipelineservice.MAPPER;

import com.example.pipelineservice.client.dto.request.CreatePipelineRequest;
import com.example.pipelineservice.client.dto.response.PipelineResponse;
import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Project;
import org.springframework.stereotype.Component;

@Component
public class PipelineMapper {

    // ─────────────────────────────
    // ENTITY MAPPING
    // ─────────────────────────────
    public Pipeline toEntity(CreatePipelineRequest request, Project project) {
        return Pipeline.builder()
                .name(request.getName())
                .jenkinsJobName(request.getJenkinsJobName()) // 🔥 FIXED
                .project(project)
                .build();
    }

    // ─────────────────────────────
    // RESPONSE MAPPING
    // ─────────────────────────────
    public PipelineResponse toResponse(Pipeline pipeline) {
        return PipelineResponse.builder()
                .id(pipeline.getId())
                .name(pipeline.getName())
                .jenkinsJobName(pipeline.getJenkinsJobName()) // 🔥 ADD THIS TOO
                .projectId(pipeline.getProject().getId())
                .status(pipeline.getStatus())
                .createdAt(pipeline.getCreatedAt())
                .build();
    }
}
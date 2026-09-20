package com.example.pipelineservice.MAPPER;

import com.example.pipelineservice.client.dto.request.CreateJobRequest;
import com.example.pipelineservice.client.dto.response.JobResponse;
import com.example.pipelineservice.entities.Job;
import com.example.pipelineservice.entities.Stage;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public Job toEntity(CreateJobRequest request, Stage stage) {

        return Job.builder()
                .name(request.getName())
                .stage(stage)
                .build();
    }

    public JobResponse toResponse(Job job) {

        return JobResponse.builder()
                .id(job.getId())
                .name(job.getName())
                .stageId(job.getStage().getId())
                .build();
    }
}
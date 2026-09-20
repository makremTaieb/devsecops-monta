package com.example.pipelineservice.MAPPER;

import com.example.pipelineservice.client.dto.response.ExecutionResponse;
import com.example.pipelineservice.client.dto.response.StageExecutionResponse;
import com.example.pipelineservice.entities.PipelineExecution;
import com.example.pipelineservice.entities.StageExecution;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExecutionMapper {

    public ExecutionResponse toResponse(PipelineExecution execution,
                                        List<StageExecution> stages) {

        return ExecutionResponse.builder()
                .id(execution.getId())
                .pipelineId(execution.getPipeline().getId())
                .commitHash(execution.getCommitHash())
                .status(execution.getStatus())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())

                // Jenkins fields (ALL STRING except build number)
                .jenkinsBuildNumber(execution.getJenkinsBuildNumber())
                .jenkinsBuildUrl(execution.getJenkinsBuildUrl())
                .jenkinsQueueId(execution.getJenkinsQueueId())

                .build();
    }
}
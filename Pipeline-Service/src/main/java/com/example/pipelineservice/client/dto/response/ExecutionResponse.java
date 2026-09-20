package com.example.pipelineservice.client.dto.response;

import com.example.pipelineservice.entities.PipelineStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExecutionResponse {
    private Long                       id;
    private Long                       pipelineId;
    private String                     commitHash;
    private PipelineStatus             status;
    private LocalDateTime              startTime;
    private LocalDateTime              endTime;
    private Integer                    jenkinsBuildNumber;
    private String                     jenkinsBuildUrl;
    private Long                    jenkinsQueueId;// link to Jenkins console
    private List<StageExecutionResponse> stages;
}
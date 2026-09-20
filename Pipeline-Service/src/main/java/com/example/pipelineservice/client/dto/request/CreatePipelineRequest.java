package com.example.pipelineservice.client.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePipelineRequest {

    @NotBlank(message = "Pipeline name is required")
    private String name;

    /**
     * Must match exactly the Jenkins job name configured on Jenkins VM.
     * Example: "devsecops-pipeline"
     */
    @NotBlank(message = "Jenkins job name is required")
    private String jenkinsJobName;
}
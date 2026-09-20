package com.example.pipelineservice.client.dto.request;

import lombok.Data;

@Data
public class ExecutionRequest {

    private String userId;
    private String commitHash;
}
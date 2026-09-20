package com.example.pipelineservice.client.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobResponse {

    private Long id;

    private String name;

    private Long stageId;
}
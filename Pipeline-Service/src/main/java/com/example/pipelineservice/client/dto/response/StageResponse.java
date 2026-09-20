package com.example.pipelineservice.client.dto.response;

import com.example.pipelineservice.entities.StageType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StageResponse {

    private Long id;

    private String name;

    private Integer orderIndex;

    private StageType type;

    private Long pipelineId;
}
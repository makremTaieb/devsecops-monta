// ---------- StageMapper.java ----------
package com.example.pipelineservice.MAPPER;

import com.example.pipelineservice.client.dto.request.CreateStageRequest;
import com.example.pipelineservice.client.dto.response.StageResponse;
import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Stage;
import org.springframework.stereotype.Component;

@Component
public class StageMapper {

    // FIX: removed mutable instance fields (request, pipeline) — race condition in singleton

    public Stage toEntity(CreateStageRequest request, Pipeline pipeline) {
        return Stage.builder()
                .name(request.getName())
                .orderIndex(request.getOrderIndex())
                .type(request.getType())
                .pipeline(pipeline)
                .build();
    }

    public StageResponse toResponse(Stage stage) {
        return StageResponse.builder()
                .id(stage.getId())
                .name(stage.getName())
                .orderIndex(stage.getOrderIndex())
                .type(stage.getType())
                .pipelineId(stage.getPipeline().getId())
                .build();
    }
}
 
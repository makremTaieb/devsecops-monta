// ---------- CreateStageRequest.java ----------
package com.example.pipelineservice.client.dto.request;

import com.example.pipelineservice.entities.StageType;
import lombok.Data;

@Data
public class CreateStageRequest {
    private String name;
    private Integer orderIndex;
    private StageType type;
}
 
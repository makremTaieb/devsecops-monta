package com.example.pipelineservice.client.dto.request;

import com.example.pipelineservice.entities.VcsType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    @NotBlank(message = "Repository URL is required")
    private String repositoryUrl;

    @NotBlank(message = "Branch is required")
    private String branch;

    // optional field
    private String owner;

    @NotNull(message = "VCS type is required")
    private VcsType vcsType;
}
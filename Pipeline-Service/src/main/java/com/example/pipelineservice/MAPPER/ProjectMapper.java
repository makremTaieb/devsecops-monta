package com.example.pipelineservice.MAPPER;

import com.example.pipelineservice.client.dto.request.CreateProjectRequest;
import com.example.pipelineservice.client.dto.response.ProjectResponse;
import com.example.pipelineservice.entities.Project;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProjectMapper {

    /**
     * Convert request → entity
     *
     * @param request   request body
     * @param createdBy username from JWT (NEVER trust request for this)
     */
    public Project toEntity(CreateProjectRequest request, String createdBy) {

        return Project.builder()
                .name(request.getName())
                .repositoryUrl(request.getRepositoryUrl())
                .branch(request.getBranch())
                .owner(request.getOwner())              // business owner (GitHub, etc.)
                .vcsType(request.getVcsType())
                .createdBy(createdBy)                  // 🔥 security owner (from JWT)
                .createdAt(LocalDateTime.now())        // ✔ set here OR in service (both OK)
                .build();
    }

    /**
     * Convert entity → response DTO
     */
    public ProjectResponse toResponse(Project project) {

        if (project == null) return null; // ✅ avoid NPE

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .repositoryUrl(project.getRepositoryUrl())
                .branch(project.getBranch())
                .owner(project.getOwner())
                .createdBy(project.getCreatedBy())     // 🔥 important for frontend filtering/debug
                .vcsType(project.getVcsType())
                .createdAt(project.getCreatedAt())
                .status(project.isDeleted() ? "DISABLED" : "ACTIVE")
                .build();
    }
}
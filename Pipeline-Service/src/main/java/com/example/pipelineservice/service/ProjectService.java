package com.example.pipelineservice.service;

import com.example.pipelineservice.client.dto.request.CreateProjectRequest;
import com.example.pipelineservice.client.dto.response.ProjectResponse;

import java.util.List;

public interface ProjectService {

    ProjectResponse createProject(CreateProjectRequest request);

    List<ProjectResponse> getProjects();

    ProjectResponse getProjectById(Long id);

    ProjectResponse updateProject(Long id, CreateProjectRequest request);

    void deleteProject(Long id);

    ProjectResponse restoreProject(Long id);
}
package com.example.pipelineservice.service;

import com.example.pipelineservice.MAPPER.ProjectMapper;
import com.example.pipelineservice.client.dto.request.CreateProjectRequest;
import com.example.pipelineservice.client.dto.response.ProjectResponse;
import com.example.pipelineservice.entities.Project;
import com.example.pipelineservice.exception.ResourceNotFoundException;
import com.example.pipelineservice.repository.ProjectRepository;
import com.example.pipelineservice.security.AuthorizationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository  projectRepository;
    private final ProjectMapper      projectMapper;
    private final AuthorizationHelper auth;   // ← injected helper

    // ─────────────────────────────
    // CREATE
    // ─────────────────────────────
    @Override
    public ProjectResponse createProject(CreateProjectRequest request) {

        // DEV and DEVOPS can create projects; AUDITOR is blocked at controller level.
        String username = auth.currentUsername();

        if (projectRepository.existsByName(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project already exists");
        }

        Project project = projectMapper.toEntity(request, username);
        return projectMapper.toResponse(projectRepository.save(project));
    }

    // ─────────────────────────────
    // GET ALL
    // ─────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects() {

        String username = auth.currentUsername();

        // ADMIN and DEVOPS see every project
        if (auth.hasGlobalAccess()) {
            return projectRepository.findByDeletedFalse()
                    .stream().map(projectMapper::toResponse).toList();
        }

        // DEV and AUDITOR see only their own projects
        return projectRepository.findByCreatedByAndDeletedFalse(username)
                .stream().map(projectMapper::toResponse).toList();
    }

    // ─────────────────────────────
    // GET BY ID
    // ─────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = findOrThrow(id);
        assertProjectAccess(project);
        return projectMapper.toResponse(project);
    }

    // ─────────────────────────────
    // UPDATE
    // ─────────────────────────────
    @Override
    public ProjectResponse updateProject(Long id, CreateProjectRequest request) {

        Project project = findOrThrow(id);
        assertProjectAccess(project);   // DEVOPS and ADMIN can update any project

        project.setName(request.getName());
        project.setRepositoryUrl(request.getRepositoryUrl());
        project.setBranch(request.getBranch());

        if (request.getOwner() != null) {
            project.setOwner(request.getOwner());
        }
        if (request.getVcsType() != null) {
            project.setVcsType(request.getVcsType());
        }

        return projectMapper.toResponse(projectRepository.save(project));
    }

    // ─────────────────────────────
    // DELETE (SOFT DELETE)
    // ─────────────────────────────
    @Override
    public void deleteProject(Long id) {
        Project project = findOrThrow(id);
        assertProjectAccess(project);

        project.setDeleted(true);
        project.setDeletedAt(LocalDateTime.now());
        projectRepository.save(project);
    }

    // ─────────────────────────────
    // RESTORE (ADMIN ONLY)
    // ─────────────────────────────
    @Override
    public ProjectResponse restoreProject(Long id) {
        if (!auth.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only ADMIN can restore deleted projects.");
        }

        Project project = findOrThrow(id);
        project.setDeleted(false);
        project.setDeletedAt(null);
        return projectMapper.toResponse(projectRepository.save(project));
    }

    // ─────────────────────────────
    // HELPERS
    // ─────────────────────────────

    private Project findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    /**
     * Gate that enforces the role matrix:
     *   ADMIN   → pass
     *   DEVOPS  → pass
     *   DEV     → pass only if createdBy == current user
     *   AUDITOR → pass only if createdBy == current user (write ops blocked at controller)
     */
    private void assertProjectAccess(Project project) {
        if (!auth.canAccessProject(project.getCreatedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: you do not have permission to access this project.");
        }
    }
}
package com.example.pipelineservice.controller;

import com.example.pipelineservice.client.dto.request.CreateProjectRequest;
import com.example.pipelineservice.client.dto.response.ProjectResponse;
import com.example.pipelineservice.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Project endpoints.
 *
 * Role enforcement at this layer (outer gate):
 *   - GET    → any authenticated user (fine-grained in service: DEV sees own only)
 *   - POST   → ADMIN, DEVOPS, DEV  (AUDITOR read-only)
 *   - PUT    → ADMIN, DEVOPS, DEV  (AUDITOR read-only)
 *   - DELETE → ADMIN, DEVOPS, DEV  (AUDITOR read-only)
 *   - RESTORE → ADMIN only
 *
 * Fine-grained ownership (DEV sees only own projects, DEVOPS sees all) is
 * enforced inside ProjectServiceImpl via AuthorizationHelper.
 */
@RestController
@RequestMapping("/api/pipeline/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // ─────────────────────────────
    // CREATE  — ADMIN, DEVOPS, DEV
    // ─────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEVOPS','DEV')")
    public ProjectResponse createProject(@RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    // ─────────────────────────────
    // GET ALL — all authenticated users
    //   ADMIN / DEVOPS → all projects
    //   DEV / AUDITOR  → own projects only (enforced in service)
    // ─────────────────────────────
    @GetMapping
    public List<ProjectResponse> getProjects() {
        return projectService.getProjects();
    }

    // ─────────────────────────────
    // GET BY ID — all authenticated users
    // ─────────────────────────────
    @GetMapping("/{id}")
    public ProjectResponse getProject(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    // ─────────────────────────────
    // UPDATE — ADMIN, DEVOPS, DEV
    // ─────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEVOPS','DEV')")
    public ProjectResponse updateProject(
            @PathVariable Long id,
            @RequestBody CreateProjectRequest request) {
        return projectService.updateProject(id, request);
    }

    // ─────────────────────────────
    // SOFT DELETE — ADMIN, DEVOPS, DEV
    // ─────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEVOPS','DEV')")
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    // ─────────────────────────────
    // RESTORE — ADMIN ONLY
    // ─────────────────────────────
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectResponse restoreProject(@PathVariable Long id) {
        return projectService.restoreProject(id);
    }
}
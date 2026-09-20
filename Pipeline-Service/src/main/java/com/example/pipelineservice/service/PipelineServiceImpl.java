package com.example.pipelineservice.service;

import com.example.pipelineservice.MAPPER.PipelineMapper;
import com.example.pipelineservice.client.AuditLogClient;
import com.example.pipelineservice.client.dto.request.CreatePipelineRequest;
import com.example.pipelineservice.client.dto.response.PipelineResponse;
import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Project;
import com.example.pipelineservice.exception.ResourceNotFoundException;
import com.example.pipelineservice.repository.PipelineRepository;
import com.example.pipelineservice.repository.ProjectRepository;
import com.example.pipelineservice.security.AuthorizationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PipelineServiceImpl implements PipelineService {

    private final PipelineRepository pipelineRepository;
    private final ProjectRepository  projectRepository;
    private final PipelineMapper     pipelineMapper;
    private final AuthorizationHelper auth;
    private final AuditLogClient     auditLogClient;    // ← NEW

    @Override
    public PipelineResponse createPipeline(Long projectId, CreatePipelineRequest request) {
        Project project = findProjectOrThrow(projectId);
        assertProjectAccess(project);

        Pipeline pipeline = pipelineMapper.toEntity(request, project);
        PipelineResponse response = pipelineMapper.toResponse(pipelineRepository.save(pipeline));

        // ── AUDIT ──
        auditLogClient.send(Map.of(
                "username",      currentUsername(),
                "action",        "PIPELINE_CREATED",
                "resource",      "PIPELINE",
                "resourceId",    response.getId(),
                "details",       "Pipeline '" + response.getName() + "' created in project " + projectId,
                "status",        "SUCCESS",
                "sourceService", "pipeline-service"
        ));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PipelineResponse> getPipelinesByProject(Long projectId) {
        Project project = findProjectOrThrow(projectId);
        assertProjectAccess(project);
        return pipelineRepository.findByProject(project)
                .stream().map(pipelineMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PipelineResponse getPipelineById(Long pipelineId) {
        Pipeline pipeline = findPipelineOrThrow(pipelineId);
        assertProjectAccess(pipeline.getProject());
        return pipelineMapper.toResponse(pipeline);
    }

    @Override
    public PipelineResponse updatePipeline(Long pipelineId, CreatePipelineRequest request) {
        Pipeline pipeline = findPipelineOrThrow(pipelineId);
        assertProjectAccess(pipeline.getProject());

        pipeline.setName(request.getName());
        pipeline.setJenkinsJobName(request.getJenkinsJobName());
        PipelineResponse response = pipelineMapper.toResponse(pipelineRepository.save(pipeline));

        // ── AUDIT ──
        auditLogClient.send(Map.of(
                "username",      currentUsername(),
                "action",        "PIPELINE_UPDATED",
                "resource",      "PIPELINE",
                "resourceId",    pipelineId,
                "details",       "Pipeline '" + response.getName() + "' updated",
                "status",        "SUCCESS",
                "sourceService", "pipeline-service"
        ));

        return response;
    }

    @Override
    public void deletePipeline(Long pipelineId) {
        Pipeline pipeline = findPipelineOrThrow(pipelineId);
        assertProjectAccess(pipeline.getProject());
        String name = pipeline.getName();
        pipelineRepository.delete(pipeline);

        // ── AUDIT ──
        auditLogClient.send(Map.of(
                "username",      currentUsername(),
                "action",        "PIPELINE_DELETED",
                "resource",      "PIPELINE",
                "resourceId",    pipelineId,
                "details",       "Pipeline '" + name + "' deleted",
                "status",        "SUCCESS",
                "sourceService", "pipeline-service"
        ));
    }

    // ── helpers ───────────────────────────────────────────────

    private Project findProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    private Pipeline findPipelineOrThrow(Long id) {
        return pipelineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found: " + id));
    }

    private void assertProjectAccess(Project project) {
        if (!auth.canAccessProject(project.getCreatedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied to project: " + project.getId());
        }
    }

    private String currentUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}

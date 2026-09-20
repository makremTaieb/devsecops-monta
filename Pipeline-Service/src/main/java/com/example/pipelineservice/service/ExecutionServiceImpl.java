package com.example.pipelineservice.service;

import com.example.pipelineservice.ExecutionTrackingService;
import com.example.pipelineservice.MAPPER.ExecutionMapper;
import com.example.pipelineservice.client.JenkinsClient;
import com.example.pipelineservice.client.dto.request.ExecutionRequest;
import com.example.pipelineservice.client.dto.response.ExecutionResponse;
import com.example.pipelineservice.entities.*;
import com.example.pipelineservice.exception.ResourceNotFoundException;
import com.example.pipelineservice.repository.PipelineExecutionRepository;
import com.example.pipelineservice.repository.PipelineRepository;
import com.example.pipelineservice.security.AuthorizationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExecutionServiceImpl implements ExecutionService {

    private final PipelineRepository          pipelineRepository;
    private final PipelineExecutionRepository executionRepository;
    private final ExecutionMapper             executionMapper;
    private final JenkinsClient              jenkinsClient;
    private final ExecutionTrackingService   trackingService;
    private final AuthorizationHelper        auth;   // ← injected helper

    @Override
    public ExecutionResponse executePipeline(Long pipelineId, ExecutionRequest request) {

        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found"));

        // ── Authorization check ───────────────────────────────────────────────────
        // Only ADMIN, DEVOPS, or the DEV who owns the project can trigger a deploy.
        // AUDITOR is explicitly denied (read-only role).
        assertCanDeploy(pipeline.getProject());

        // 1. CREATE EXECUTION RECORD
        PipelineExecution execution = PipelineExecution.builder()
                .pipeline(pipeline)
                .triggeredBy(request.getUserId())
                .commitHash(request.getCommitHash())
                .status(PipelineStatus.PENDING)
                .startTime(LocalDateTime.now())
                .build();

        execution = executionRepository.save(execution);

        // 🔥 VERY IMPORTANT → avoid async race condition
        executionRepository.flush();

        // 2. TRIGGER JENKINS
        String queueUrl = jenkinsClient.triggerJob(
                pipeline.getJenkinsJobName(),
                execution
        );

        String queueId = jenkinsClient.extractQueueId(queueUrl);

        execution.setJenkinsQueueId(Long.parseLong(queueId));
        executionRepository.save(execution);

        // 3. ASYNC TRACKING
        trackingService.trackAsync(
                execution.getId(),
                pipeline.getId(),
                queueId
        );

        // 4. RETURN IMMEDIATELY (202 ACCEPTED returned by controller)
        return executionMapper.toResponse(execution, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExecutionResponse> getExecutionsByPipeline(Long pipelineId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found"));

        // Access check: DEVOPS/ADMIN see all, DEV/AUDITOR see only their own
        assertCanAccessProject(pipeline.getProject());

        return executionRepository.findByPipelineOrderByStartTimeDesc(pipeline)
                .stream()
                .map(exec -> executionMapper.toResponse(exec, List.of()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutionResponse getExecutionById(Long executionId) {
        PipelineExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));

        // Access check on the parent project
        assertCanAccessProject(execution.getPipeline().getProject());

        return executionMapper.toResponse(execution, List.of());
    }

    @Override
    public void updateStatus(Long executionId, PipelineStatus status) {
        // Internal service method — called by tracking/webhook, no user check needed
        PipelineExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));

        execution.setStatus(status);
        executionRepository.save(execution);
    }

    // ── helpers ───────────────────────────────────────────────

    /**
     * Deployment trigger gate.
     *
     * ADMIN  → allowed
     * DEVOPS → allowed (this is the primary deploy role)
     * DEV    → allowed only if they own the project
     * AUDITOR → ALWAYS denied (read-only role, cannot trigger deployments)
     */
    private void assertCanDeploy(Project project) {
        if (auth.isAuditor()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "AUDITOR role cannot trigger pipeline executions.");
        }
        if (!auth.canAccessProject(project.getCreatedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: you do not have permission to deploy this pipeline.");
        }
    }

    /**
     * Read access gate — same as project access.
     */
    private void assertCanAccessProject(Project project) {
        if (!auth.canAccessProject(project.getCreatedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: you do not have permission to view this execution.");
        }
    }
}
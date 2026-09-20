package com.example.pipelineservice.controller;

import com.example.pipelineservice.client.dto.request.ExecutionRequest;
import com.example.pipelineservice.client.dto.request.StatusUpdateRequest;
import com.example.pipelineservice.client.dto.response.ExecutionResponse;
import com.example.pipelineservice.entities.PipelineStatus;
import com.example.pipelineservice.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Execution endpoints.
 *
 * Role enforcement:
 *   POST /{pipelineId}        → ADMIN, DEVOPS, DEV  (AUDITOR read-only, cannot deploy)
 *   GET  /{executionId}       → all authenticated (fine-grained check in service)
 *   GET  /pipeline/{id}       → all authenticated (fine-grained check in service)
 */
@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    /**
     * Trigger a pipeline execution.
     *
     * DEVOPS role is the primary actor here — they deploy.
     * Returns 202 ACCEPTED with status=PENDING.
     * Poll GET /api/executions/{id} to track progress.
     */
    @PostMapping("/{pipelineId}")
    @PreAuthorize("hasAnyRole('ADMIN','DEVOPS','DEV')")
    public ResponseEntity<ExecutionResponse> execute(
            @PathVariable Long pipelineId,
            @RequestBody ExecutionRequest request) {

        ExecutionResponse response = executionService.executePipeline(pipelineId, request);

        URI pollingUri = URI.create("/api/executions/" + response.getId());

        return ResponseEntity
                .accepted()           // 202 — tells the caller "in progress, poll me"
                .location(pollingUri) // Location: /api/executions/95
                .body(response);
    }

    /**
     * Poll this to get live status: PENDING → RUNNING → SUCCESS / FAILED
     */
    @GetMapping("/{executionId}")
    public ResponseEntity<ExecutionResponse> getById(@PathVariable Long executionId) {
        return ResponseEntity.ok(executionService.getExecutionById(executionId));
    }

    /**
     * Get all executions for a pipeline, newest first.
     */
    @GetMapping("/pipeline/{pipelineId}")
    public ResponseEntity<List<ExecutionResponse>> getByPipeline(@PathVariable Long pipelineId) {
        return ResponseEntity.ok(executionService.getExecutionsByPipeline(pipelineId));
    }

    /**
     * Called by Jenkins post{} block to report final build status (SUCCESS / FAILED).
     * No auth required — Jenkins calls this from inside the cluster or via NodePort.
     * We use a dedicated internal endpoint to avoid requiring a JWT token from Jenkins.
     */
    @PutMapping("/{executionId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long executionId,
            @RequestBody StatusUpdateRequest request) {
        executionService.updateStatus(executionId, request.getStatus());
        return ResponseEntity.ok().build();
    }
}
package com.example.pipelineservice;

import com.example.pipelineservice.client.JenkinsClient;
import com.example.pipelineservice.client.NotificationClient;
import com.example.pipelineservice.entities.*;
import com.example.pipelineservice.repository.PipelineExecutionRepository;
import com.example.pipelineservice.repository.PipelineRepository;
import com.example.pipelineservice.service.ExecutionUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionTrackingService {

    private final JenkinsClient               jenkinsClient;
    private final PipelineExecutionRepository executionRepository;
    private final PipelineRepository          pipelineRepository;
    private final ExecutionUpdateService      executionUpdateService;
    private final NotificationClient          notificationClient;

    @Async("pipelineExecutor")
    public void trackAsync(Long executionId, Long pipelineId, String queueId) {

        log.info("[TRACK] Start execution {}", executionId);

        PipelineExecution execution = waitForExecution(executionId);
        Pipeline pipeline = pipelineRepository.findById(pipelineId).orElse(null);

        if (execution == null || pipeline == null) {
            log.error("[TRACK] Execution or pipeline not found");
            return;
        }

        Integer buildNumber = null;

        // ── WAIT FOR BUILD NUMBER ──────────────────────────────────
        for (int i = 0; i < 20; i++) {
            buildNumber = jenkinsClient.getBuildNumber(queueId, pipeline.getJenkinsJobName());
            if (buildNumber != null) {
                log.info("[TRACK] Build number found: {}", buildNumber);
                break;
            }
            log.info("[TRACK] Waiting for build number... attempt {}", i);
            sleep(3000);
        }

        // ── TIMEOUT ────────────────────────────────────────────────
        if (buildNumber == null) {
            log.error("[TRACK] Could not resolve build number for execution {}", executionId);
            executionUpdateService.updateStatus(executionId, PipelineStatus.FAILED);
            sendNotification(execution, pipeline, PipelineStatus.FAILED);
            return;
        }

        // ── MARK RUNNING ───────────────────────────────────────────
        executionUpdateService.updateRunning(executionId, pipeline, buildNumber);

        // ── POLL UNTIL DONE ────────────────────────────────────────
        PipelineStatus status = jenkinsClient.pollBuildStatus(
                pipeline.getJenkinsJobName(),
                buildNumber
        );

        // ── SAVE FINAL STATUS ──────────────────────────────────────
        executionUpdateService.updateFinal(executionId, status);

        // ── SEND NOTIFICATION ──────────────────────────────────────
        sendNotification(execution, pipeline, status);

        log.info("[TRACK] Execution {} finished with {}", executionId, status);
    }

    // ── Notification helper ────────────────────────────────────────
    private void sendNotification(PipelineExecution execution, Pipeline pipeline, PipelineStatus status) {
        try {
            // Re-fetch with project eagerly loaded to avoid LazyInitializationException
            pipeline = pipelineRepository.findByIdWithProject(pipeline.getId()).orElse(pipeline);

            String eventType = status == PipelineStatus.SUCCESS
                    ? "PIPELINE_SUCCESS" : "PIPELINE_FAILED";

            String projectName = pipeline.getProject() != null
                    ? pipeline.getProject().getName() : "Unknown";

            Map<String, Object> payload = Map.of(
                "pipelineExecutionId", execution.getId(),
                "projectId",           pipeline.getProject() != null ? pipeline.getProject().getId() : 0L,
                "eventType",           eventType,
                "projectName",         projectName,
                "branch",              pipeline.getProject() != null && pipeline.getProject().getBranch() != null
                                           ? pipeline.getProject().getBranch() : "main",
                "commitHash",          execution.getCommitHash() != null ? execution.getCommitHash() : "N/A",
                "triggeredBy",         execution.getTriggeredBy() != null ? execution.getTriggeredBy() : "system"
            );

            notificationClient.sendPipelineEvent(payload);
        } catch (Exception e) {
            log.warn("[TRACK] Failed to send notification (non-critical): {}", e.getMessage());
        }
    }

    // ── Safe load ──────────────────────────────────────────────────
    private PipelineExecution waitForExecution(Long id) {
        for (int i = 0; i < 5; i++) {
            var exec = executionRepository.findById(id);
            if (exec.isPresent()) return exec.get();
            sleep(500);
        }
        return null;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

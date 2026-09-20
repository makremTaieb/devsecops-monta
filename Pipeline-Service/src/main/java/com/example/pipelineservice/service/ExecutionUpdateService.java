package com.example.pipelineservice.service;

import com.example.pipelineservice.client.JenkinsClient;
import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.PipelineExecution;
import com.example.pipelineservice.entities.PipelineStatus;
import com.example.pipelineservice.repository.PipelineExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExecutionUpdateService {

    private final PipelineExecutionRepository executionRepository;
    private final JenkinsClient jenkinsClient;

    @Transactional
    public void updateRunning(Long id, Pipeline pipeline, int buildNumber) {
        executionRepository.findById(id).ifPresent(exec -> {
            exec.setStatus(PipelineStatus.RUNNING);
            exec.setJenkinsBuildNumber(buildNumber);
            exec.setJenkinsBuildUrl(
                    jenkinsClient.buildConsoleUrl(
                            pipeline.getJenkinsJobName(),
                            buildNumber
                    )
            );
            executionRepository.save(exec);
        });
    }

    @Transactional
    public void updateFinal(Long id, PipelineStatus status) {
        executionRepository.findById(id).ifPresent(exec -> {
            exec.setStatus(status);
            exec.setEndTime(LocalDateTime.now());
            executionRepository.save(exec);
        });
    }

    @Transactional
    public void updateStatus(Long id, PipelineStatus status) {
        executionRepository.findById(id).ifPresent(exec -> {
            exec.setStatus(status);
            exec.setEndTime(LocalDateTime.now());
            executionRepository.save(exec);
        });
    }
}
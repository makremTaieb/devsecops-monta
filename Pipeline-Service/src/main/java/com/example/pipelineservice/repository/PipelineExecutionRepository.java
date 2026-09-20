package com.example.pipelineservice.repository;

import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.PipelineExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PipelineExecutionRepository extends JpaRepository<PipelineExecution, Long> {

    List<PipelineExecution> findByPipelineOrderByStartTimeDesc(Pipeline pipeline);

    Optional<PipelineExecution> findByJenkinsQueueId(Long jenkinsQueueId);

    Optional<PipelineExecution> findByJenkinsBuildNumber(Integer buildNumber);

    // ✅ ADD THIS (fix your service error)
    List<PipelineExecution> findByPipeline(Pipeline pipeline);
}
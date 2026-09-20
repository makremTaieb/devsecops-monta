package com.example.pipelineservice.repository;

import com.example.pipelineservice.entities.PipelineExecution;
import com.example.pipelineservice.entities.StageExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StageExecutionRepository extends JpaRepository<StageExecution, Long> {
    List<StageExecution> findByPipelineExecution(PipelineExecution execution);
}
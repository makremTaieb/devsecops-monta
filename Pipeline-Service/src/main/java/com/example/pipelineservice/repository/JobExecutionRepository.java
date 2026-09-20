package com.example.pipelineservice.repository;

import com.example.pipelineservice.entities.JobExecution;
import com.example.pipelineservice.entities.StageExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// FIX: was a plain interface — must extend JpaRepository to be a Spring bean
public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {
    List<JobExecution> findByStageExecution(StageExecution stageExecution);
}
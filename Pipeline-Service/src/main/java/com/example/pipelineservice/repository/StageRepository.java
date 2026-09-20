package com.example.pipelineservice.repository;

import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StageRepository extends JpaRepository<Stage, Long> {
    List<Stage> findByPipelineOrderByOrderIndexAsc(Pipeline pipeline);
}
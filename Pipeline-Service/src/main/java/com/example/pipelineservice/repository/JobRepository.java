package com.example.pipelineservice.repository;

import com.example.pipelineservice.entities.Job;
import com.example.pipelineservice.entities.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStageOrderByOrderIndexAsc(Stage stage);
}
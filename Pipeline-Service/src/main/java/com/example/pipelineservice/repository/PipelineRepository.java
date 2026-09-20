package com.example.pipelineservice.repository;

import com.example.pipelineservice.entities.Pipeline;
import com.example.pipelineservice.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PipelineRepository extends JpaRepository<Pipeline, Long> {
    List<Pipeline> findByProject(Project project);
    Optional<Pipeline> findFirstByProject(Project project);
    Optional<Pipeline> findByJenkinsJobName(String jenkinsJobName);

    @Query("SELECT p FROM Pipeline p LEFT JOIN FETCH p.project WHERE p.id = :id")
    Optional<Pipeline> findByIdWithProject(@Param("id") Long id);
}
package com.example.pipelineservice.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DESIGN DECISION: JobExecution is NOT used in the execution flow.
 * Jenkins handles job-level tracking natively.
 * This entity exists ONLY as an audit log if Jenkins sends a detailed callback.
 *
 * FIX: Removed from ExecutionService entirely. No more Java-side job orchestration.
 */
@Entity
@Table(name = "job_execution")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_execution_id")
    private StageExecution stageExecution;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PipelineStatus status = PipelineStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String consoleLog;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
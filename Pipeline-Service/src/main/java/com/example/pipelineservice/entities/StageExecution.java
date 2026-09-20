package com.example.pipelineservice.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Records the status of one Stage within one PipelineExecution.
 * Populated when Jenkins sends a callback OR when we poll Jenkins API.
 *
 * FIX: Removed @Data, added @PrePersist, @Builder.Default
 */
@Entity
@Table(name = "stage_execution")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StageExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_execution_id", nullable = false)
    private PipelineExecution pipelineExecution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PipelineStatus status = PipelineStatus.PENDING;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @PrePersist
    public void prePersist() {
        if (startTime == null) startTime = LocalDateTime.now();
    }
}
package com.example.pipelineservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pipeline_execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // TRIGGER INFO
    // =========================

    // ⚠️ FIX: should be String (userId/email/token usually NOT numeric safe)
    private String triggeredBy;

    private String commitHash;

    // =========================
    // JENKINS TRACKING
    // =========================

    /**
     * Jenkins queue item ID (/queue/item/{id})
     */
    @Column(name = "jenkins_queue_id")
    private Long jenkinsQueueId;

    /**
     * Jenkins build number (assigned after execution starts)
     */
    @Column(name = "jenkins_build_number")
    private Integer jenkinsBuildNumber;

    /**
     * Jenkins build URL
     */
    @Column(name = "jenkins_build_url")
    private String jenkinsBuildUrl;

    // =========================
    // STATUS / TIMING
    // =========================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PipelineStatus status = PipelineStatus.PENDING;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    // =========================
    // RELATION
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private Pipeline pipeline;

    // =========================
    // AUTO INIT
    // =========================

    @PrePersist
    public void prePersist() {

        if (startTime == null) {
            startTime = LocalDateTime.now();
        }

        if (status == null) {
            status = PipelineStatus.PENDING;
        }
    }
}
package com.example.pipelineservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FIXES:
 * - Added jenkinsJobName: the Jenkins job this pipeline maps to
 * - Removed @Data (Lombok conflict)
 * - @Builder.Default so status and lists are never null
 * - @PrePersist for automatic timestamps
 *
 * NOTE: Stage/Job entities are kept as METADATA only.
 * Jenkins owns all actual execution — stages here describe pipeline structure.
 */
@Entity
@Table(name = "pipeline")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Pipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * Jenkins job name — must match exactly the job defined on Jenkins VM.
     * Example: "devsecops-pipeline"
     */
    @NotBlank(message = "Jenkins job name is required")
    private String jenkinsJobName;
    @Enumerated(EnumType.STRING)

    @Column(nullable = false)
    @Builder.Default
    private PipelineStatus status = PipelineStatus.CREATED;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Stage> stages = new ArrayList<>();

    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<PipelineExecution> executions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status    == null) status    = PipelineStatus.CREATED;
    }
}
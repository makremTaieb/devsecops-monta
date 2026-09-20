package com.example.pipelineservice.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage = pipeline step METADATA.
 * Jenkins reads this structure through its Jenkinsfile.
 * No execution logic lives here — Jenkins handles that.
 *
 * FIX: Removed @Data (Lombok @Builder conflict with collections)
 */
@Entity
@Table(name = "stage")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StageType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private Pipeline pipeline;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Job> jobs = new ArrayList<>();
}
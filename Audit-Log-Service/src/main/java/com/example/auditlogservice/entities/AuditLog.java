package com.example.auditlogservice.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_username", columnList = "username"),
        @Index(name = "idx_audit_action",   columnList = "action"),
        @Index(name = "idx_audit_resource", columnList = "resource"),
        @Index(name = "idx_audit_ts",       columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** When the action happened */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** ID of the authenticated user (null for anonymous/system) */
    private Long userId;

    /** Username of the actor */
    @Column(length = 100)
    private String username;

    /** Action performed — use ActionType enum values */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private ActionType action;

    /** Resource type acted on (e.g. "USER", "PIPELINE", "PROJECT", "SECURITY_SCAN") */
    @Column(length = 60)
    private String resource;

    /** ID of the resource (optional) */
    private Long resourceId;

    /** Human-readable description of what happened */
    @Column(length = 512)
    private String details;

    /** Client IP address */
    @Column(length = 60)
    private String ipAddress;

    /** SUCCESS or FAILURE */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionStatus status;

    /** Service that emitted this log (auth-service, pipeline-service, …) */
    @Column(length = 60)
    private String sourceService;
}

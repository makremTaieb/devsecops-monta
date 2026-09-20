package com.example.auditlogservice.service;

import com.example.auditlogservice.dto.AuditLogRequest;
import com.example.auditlogservice.dto.AuditLogResponse;
import com.example.auditlogservice.entities.ActionStatus;
import com.example.auditlogservice.entities.ActionType;
import com.example.auditlogservice.entities.AuditLog;
import com.example.auditlogservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository repo;

    @Override
    public AuditLogResponse record(AuditLogRequest req) {
        AuditLog log = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .userId(req.getUserId())
                .username(req.getUsername())
                .action(req.getAction())
                .resource(req.getResource())
                .resourceId(req.getResourceId())
                .details(req.getDetails())
                .ipAddress(req.getIpAddress())
                .status(req.getStatus())
                .sourceService(req.getSourceService())
                .build();

        AuditLog saved = repo.save(log);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            String username, ActionType action, String resource,
            ActionStatus status, LocalDateTime from, LocalDateTime to,
            Pageable pageable) {

        return repo.search(username, action, resource, status, from, to, pageable)
                   .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditLogResponse> findById(Long id) {
        return repo.findById(id).map(this::toResponse);
    }

    // ── mapper ──────────────────────────────────────────────────
    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .timestamp(a.getTimestamp())
                .userId(a.getUserId())
                .username(a.getUsername())
                .action(a.getAction())
                .resource(a.getResource())
                .resourceId(a.getResourceId())
                .details(a.getDetails())
                .ipAddress(a.getIpAddress())
                .status(a.getStatus())
                .sourceService(a.getSourceService())
                .build();
    }
}

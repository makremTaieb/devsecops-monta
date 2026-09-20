package com.example.auditlogservice.service;

import com.example.auditlogservice.dto.AuditLogRequest;
import com.example.auditlogservice.dto.AuditLogResponse;
import com.example.auditlogservice.entities.ActionStatus;
import com.example.auditlogservice.entities.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuditLogService {

    AuditLogResponse record(AuditLogRequest request);

    Page<AuditLogResponse> search(
            String username,
            ActionType action,
            String resource,
            ActionStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    Optional<AuditLogResponse> findById(Long id);
}

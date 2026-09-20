package com.example.auditlogservice.controller;

import com.example.auditlogservice.dto.AuditLogRequest;
import com.example.auditlogservice.dto.AuditLogResponse;
import com.example.auditlogservice.entities.ActionStatus;
import com.example.auditlogservice.entities.ActionType;
import com.example.auditlogservice.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService service;

    /**
     * Internal endpoint — called by other microservices to record an event.
     * No auth required so backend services can call it with just the service token.
     * Gateway routes this as a back-channel; it should NOT be reachable from outside.
     */
    @PostMapping("/log")
    public ResponseEntity<AuditLogResponse> record(@Valid @RequestBody AuditLogRequest request) {
        return ResponseEntity.ok(service.record(request));
    }

    /**
     * Query endpoint — requires ADMIN or DEVOPS role.
     * All parameters are optional; omit any to skip that filter.
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAnyAuthority('ADMIN','DEVOPS')")
    public ResponseEntity<Page<AuditLogResponse>> search(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) ActionType action,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) ActionStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(service.search(username, action, resource, status, from, to, pageable));
    }

    /**
     * Fetch a single log entry by id.
     */
    @GetMapping("/logs/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','DEVOPS')")
    public ResponseEntity<AuditLogResponse> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

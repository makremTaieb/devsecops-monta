package com.example.auditlogservice.repository;

import com.example.auditlogservice.entities.ActionStatus;
import com.example.auditlogservice.entities.ActionType;
import com.example.auditlogservice.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:username   IS NULL OR LOWER(a.username)   LIKE LOWER(CONCAT('%',:username,'%')))
              AND (:action     IS NULL OR a.action            = :action)
              AND (:resource   IS NULL OR a.resource          = :resource)
              AND (:status     IS NULL OR a.status            = :status)
              AND (:from       IS NULL OR a.timestamp        >= :from)
              AND (:to         IS NULL OR a.timestamp        <= :to)
            """)
    Page<AuditLog> search(
            @Param("username") String username,
            @Param("action")   ActionType action,
            @Param("resource") String resource,
            @Param("status")   ActionStatus status,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to,
            Pageable pageable
    );
}

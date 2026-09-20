# Audit Logs Enhancement — DevSecOps STB Platform

## What's included in this package

```
audit-enhancement/
│
├── Audit-Log-Service/                  ← NEW microservice (Spring Boot, port 8085)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/.../
│       ├── AuditLogServiceApplication.java
│       ├── controller/AuditLogController.java
│       ├── dto/AuditLogRequest.java
│       ├── dto/AuditLogResponse.java
│       ├── entities/AuditLog.java
│       ├── entities/ActionType.java
│       ├── entities/ActionStatus.java
│       ├── repository/AuditLogRepository.java
│       ├── service/AuditLogService.java
│       ├── service/AuditLogServiceImpl.java
│       ├── security/JwtAuthFilter.java
│       ├── security/JwtTokenParser.java
│       └── security/SecurityConfig.java
│
├── Auth-Service-patches/               ← Drop-in replacements for Auth-Service
│   ├── AuditLogClient.java             → authservice/client/
│   ├── AppConfig.java                  → authservice/config/
│   ├── AuthServiceImpl.java            → authservice/service/  (replaces existing)
│   └── application.properties          → Auth-Service/src/main/resources/ (replaces)
│
├── Gateway-patches/
│   └── application.properties          → Gateway/src/main/resources/ (replaces)
│                                         Adds route: /api/audit/** → audit-log-service
│
├── k8s/
│   ├── apps/audit.yaml                 → NEW K8s Deployment + Service
│   └── infra/mysql.yaml                → Updated: adds auditlogdb
│
├── frontend/src/app/
│   ├── core/models/audit.model.ts      → NEW
│   ├── core/services/audit.service.ts  → NEW
│   ├── features/audit-logs/            → NEW component (HTML + TS + SCSS)
│   ├── layout/shell.routes.ts          → Updated: adds /audit-logs route
│   ├── layout/shell.component.ts       → Updated: adds Audit Logs nav item
│   └── environments/
│       ├── environment.ts              → Updated: adds apiAudit
│       └── environment.prod.ts         → Updated: adds apiAudit
│
├── Jenkinsfile                         → Updated: audit-service build/push/deploy
│                                         + audit events on scan/deploy outcomes
│
└── VM-SETUP-GUIDE.sh                   → Step-by-step commands for both VMs
```

## Architecture

```
Jenkins       → POST /api/audit/log  (scan blocked / pipeline success/fail)
Auth-Service  → POST /api/audit/log  (login / register / refresh — async, fire-and-forget)
                        ↓
              Gateway (30080)
                        ↓
              Audit-Log-Service (8085, ClusterIP)
                        ↓
              MySQL → auditlogdb.audit_logs
                        ↑
              Frontend → GET /api/audit/logs  (ADMIN + DEVOPS only)
```

## Database schema (auto-created by Hibernate)

```
audit_logs
  id            BIGINT PK AUTO_INCREMENT
  timestamp     DATETIME       NOT NULL
  user_id       BIGINT
  username      VARCHAR(100)
  action        VARCHAR(60)    NOT NULL  -- ActionType enum
  resource      VARCHAR(60)
  resource_id   BIGINT
  details       VARCHAR(512)
  ip_address    VARCHAR(60)
  status        VARCHAR(20)    NOT NULL  -- SUCCESS / FAILURE
  source_service VARCHAR(60)
```

## API Endpoints

| Method | Path                | Auth          | Description                        |
|--------|---------------------|---------------|------------------------------------|
| POST   | /api/audit/log      | None (internal) | Record an audit event            |
| GET    | /api/audit/logs     | ADMIN/DEVOPS  | Search logs with filters + paging  |
| GET    | /api/audit/logs/:id | ADMIN/DEVOPS  | Fetch single log entry             |

### GET /api/audit/logs query params
- `username` — partial match
- `action`   — exact ActionType (e.g. USER_LOGIN)
- `resource` — exact (USER, PIPELINE, PROJECT, SECURITY_SCAN)
- `status`   — SUCCESS or FAILURE
- `from`     — ISO datetime (e.g. 2026-05-01T00:00:00)
- `to`       — ISO datetime
- `page`     — 0-based (default 0)
- `size`     — (default 20)

## Actions tracked

| Source         | Actions                                                    |
|----------------|------------------------------------------------------------|
| Auth-Service   | USER_LOGIN, USER_REGISTER, USER_REFRESH_TOKEN              |
| Jenkins        | SECURITY_SCAN_COMPLETED, SECURITY_SCAN_BLOCKED, PIPELINE_TRIGGERED, PIPELINE_ABORTED |

## Roles

- `POST /api/audit/log` — open (no auth) — so any backend service can post
- `GET /api/audit/logs` — requires ADMIN or DEVOPS JWT role
- Frontend Audit Logs page — guarded by roleGuard(['ADMIN','DEVOPS'])

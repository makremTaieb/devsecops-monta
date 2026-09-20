# 📋 VM Setup Guide — Audit Logs Feature
# DevSecOps STB Platform | 2026

## Overview of changes
- New microservice: Audit-Log-Service (port 8085)
- New database: auditlogdb
- New K8s deployment + service in namespace `apps`
- New gateway route: /api/audit/**
- Auth-Service now emits login/register/refresh events
- Jenkins pipeline emits scan/deploy events
- Frontend: new Audit Logs page (ADMIN + DEVOPS roles)

---

## 1. K8S VM (192.168.56.20)
# ─────────────────────────────────────────────────────────────

### 1.1 — Create auditlogdb in MySQL (one-time)
# The mysql-init ConfigMap now includes the CREATE statement,
# but if MySQL is already running you need to create it manually.

kubectl exec -n infra deployment/mysql -- \
  mysql -uroot -p"${DB_PASSWORD:?Set DB_PASSWORD first}" \
  -e "CREATE DATABASE IF NOT EXISTS auditlogdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Verify:
kubectl exec -n infra deployment/mysql -- \
  mysql -uroot -p"${DB_PASSWORD:?Set DB_PASSWORD first}" -e "SHOW DATABASES;"


### 1.2 — Apply updated mysql ConfigMap (adds auditlogdb to init script for future pods)
kubectl apply -f k8s/infra/mysql.yaml


### 1.3 — Apply the new Audit-Log-Service manifest
kubectl apply -f k8s/apps/audit.yaml

# Check it's running:
kubectl get pods -n apps -l app=audit-log-service
kubectl logs -n apps deployment/audit-log-service --tail=50


### 1.4 — Apply updated Gateway (new route for /api/audit/**)
# Copy Gateway/src/main/resources/application.properties from
# Gateway-patches/application.properties, then rebuild + push Gateway.
# OR — if you prefer to hot-patch without a rebuild:
kubectl set env deployment/api-gateway -n infra \
  SPRING_CLOUD_GATEWAY_ROUTES_7_ID=audit-log-service \
  SPRING_CLOUD_GATEWAY_ROUTES_7_URI=lb://audit-log-service \
  SPRING_CLOUD_GATEWAY_ROUTES_7_PREDICATES_0="Path=/api/audit/**"
# NOTE: the above env-var trick does not work for gateway routes.
#       Rebuild + redeploy Gateway is the correct path (Jenkins handles this).


### 1.5 — Verify namespaces and services after deploy
kubectl get pods    -n apps
kubectl get svc     -n apps
kubectl get pods    -n infra

# Audit-Log-Service actuator health:
curl http://192.168.56.20:$(kubectl get svc audit-log-service -n apps -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")/actuator/health
# Or from inside the cluster:
kubectl exec -n apps deployment/audit-log-service -- \
  wget -qO- http://localhost:8085/actuator/health


### 1.6 — Test audit log write (from K8s VM or Jenkins VM)
curl -X POST http://192.168.56.20:30080/api/audit/log \
  -H 'Content-Type: application/json' \
  -d '{
    "action":        "SYSTEM_EVENT",
    "resource":      "USER",
    "details":       "Manual audit test",
    "status":        "SUCCESS",
    "sourceService": "manual-test"
  }'

# Expected: JSON with id + timestamp


### 1.7 — Test audit log read (requires ADMIN JWT)
# 1. Login via gateway to get a token:
TOKEN=$(curl -s -X POST http://192.168.56.20:30080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<your-admin-password>"}' | jq -r '.accessToken')

# 2. Query logs:
curl -H "Authorization: Bearer $TOKEN" \
  "http://192.168.56.20:30080/api/audit/logs?page=0&size=10"


---

## 2. JENKINS VM (192.168.56.10)
# ─────────────────────────────────────────────────────────────

### 2.1 — Copy new Jenkinsfile to workspace
# Replace /var/lib/jenkins/workspace/PFE-2026/Jenkinsfile
# with the Jenkinsfile from this package.
# If you use SCM checkout, just commit it to your Git repo.

cp Jenkinsfile /var/lib/jenkins/workspace/PFE-2026/Jenkinsfile


### 2.2 — Make sure the Nexus registry is accessible
# The new audit-service image tag is:
#   192.168.56.10:5000/audit-log-service:latest
# No credential changes needed — existing nexus-credentials are reused.


### 2.3 — Verify kubectl is configured on Jenkins VM
# Jenkins stages run kubectl against the K8s VM.
# Check:
kubectl --kubeconfig /var/lib/jenkins/.kube/config get nodes
# If not configured, copy the kubeconfig from the K8s VM:
#   scp k8s-user@192.168.56.20:~/.kube/config /var/lib/jenkins/.kube/config
#   chown jenkins:jenkins /var/lib/jenkins/.kube/config


### 2.4 — Trigger a new pipeline build
# In the Jenkins UI: PFE-2026 → Build with Parameters
# Provide EXECUTION_ID and PROJECT_ID as usual.
# New stages will automatically:
#   - Build + push audit-log-service Docker image
#   - Apply k8s/apps/audit.yaml
#   - Restart the audit-log-service deployment
#   - Post pipeline result as an audit event to /api/audit/log


---

## 3. SOURCE CODE CHANGES — What to copy where
# ─────────────────────────────────────────────────────────────

### 3.1 — New service (copy entire folder)
# Audit-Log-Service/  →  <project-root>/Audit-Log-Service/
# Build with Maven: cd Audit-Log-Service && mvn clean package -DskipTests


### 3.2 — Auth-Service patches
# Copy these files INTO the Auth-Service:
#
#   Auth-Service-patches/AuditLogClient.java
#     → Auth-Service/src/main/java/com/example/authservice/client/AuditLogClient.java
#
#   Auth-Service-patches/AppConfig.java
#     → Auth-Service/src/main/java/com/example/authservice/config/AppConfig.java
#
#   Auth-Service-patches/AuthServiceImpl.java
#     → Auth-Service/src/main/java/com/example/authservice/service/AuthServiceImpl.java  (REPLACE)
#
#   Auth-Service-patches/application.properties
#     → Auth-Service/src/main/resources/application.properties  (REPLACE)
#
# No pom.xml changes needed — RestTemplate is already in spring-boot-starter-web.


### 3.3 — Gateway patch
# Copy:
#   Gateway-patches/application.properties
#     → Gateway/src/main/resources/application.properties  (REPLACE)


### 3.4 — Frontend patches
# Copy:
#   frontend/src/app/core/models/audit.model.ts             (NEW)
#   frontend/src/app/core/services/audit.service.ts         (NEW)
#   frontend/src/app/features/audit-logs/                   (NEW folder)
#   frontend/src/app/layout/shell.routes.ts                 (REPLACE)
#   frontend/src/app/layout/shell.component.ts              (REPLACE)
#   frontend/src/environments/environment.ts                (REPLACE)
#   frontend/src/environments/environment.prod.ts           (REPLACE)


### 3.5 — K8s manifests
#   k8s/apps/audit.yaml       (NEW — copy to repo and workspace)
#   k8s/infra/mysql.yaml      (REPLACE — adds auditlogdb)
#   Jenkinsfile               (REPLACE)


---

## 4. Ports & Network Reference
# ─────────────────────────────────────────────────────────────
#
# Service              | ClusterIP Port | NodePort (if any)
# ---------------------|----------------|-------------------
# Auth-Service         | 8081           | —
# Pipeline-Service     | 8082           | —
# Security-Service     | 8083           | 30083
# Gateway              | 8084           | 30080
# Audit-Log-Service    | 8085           | —  (internal only)
# Notification-Service | 8087           | —
# Eureka-Server        | 8761           | 30761
#
# All external traffic → Gateway (30080) → routes to services


---

## 5. Quick Rollback
# ─────────────────────────────────────────────────────────────
# If something goes wrong with the audit service:

# Scale it down (other services remain unaffected — audit is fire-and-forget):
kubectl scale deployment/audit-log-service -n apps --replicas=0

# Auth-Service will log a warning per request but continue operating normally.
# Rollback Auth-Service to previous image:
kubectl rollout undo deployment/auth-service -n apps

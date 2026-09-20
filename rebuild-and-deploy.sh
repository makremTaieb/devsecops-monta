#!/bin/bash
# ============================================================
# REBUILD & REDEPLOY — fixes audit logs + notifications
# Run from VM1 (Jenkins machine)
# ============================================================
set -e

WORKSPACE=/var/lib/jenkins/workspace/PFE-2026
REGISTRY=192.168.56.10:5000

cd $WORKSPACE

echo "================================================"
echo "STEP 1 — Build changed services"
echo "================================================"

# Auth-Service (audit calls added)
echo "→ Building auth-service..."
cd $WORKSPACE/Auth-Service && mvn clean package -DskipTests -q
cd $WORKSPACE

# Pipeline-Service (NotificationClient + audit calls added)
echo "→ Building pipeline-service..."
cd $WORKSPACE/Pipeline-Service && mvn clean package -DskipTests -q
cd $WORKSPACE

# Audit-Log-Service (new service)
echo "→ Building audit-log-service..."
cd $WORKSPACE/Audit-Log-Service && mvn clean package -DskipTests -q
cd $WORKSPACE

# Gateway (audit route added)
echo "→ Building api-gateway..."
cd $WORKSPACE/Gateway && mvn clean package -DskipTests -q
cd $WORKSPACE

echo "================================================"
echo "STEP 2 — Build & push Docker images"
echo "================================================"

docker build -t $REGISTRY/auth-service:latest      ./Auth-Service/
docker push $REGISTRY/auth-service:latest

docker build -t $REGISTRY/pipeline-service:latest  ./Pipeline-Service/
docker push $REGISTRY/pipeline-service:latest

docker build -t $REGISTRY/audit-log-service:latest ./Audit-Log-Service/
docker push $REGISTRY/audit-log-service:latest

docker build -t $REGISTRY/api-gateway:latest        ./Gateway/
docker push $REGISTRY/api-gateway:latest

echo "================================================"
echo "STEP 3 — Apply manifests & force rollout"
echo "================================================"

kubectl apply -f k8s/infra/gateway.yaml
kubectl apply -f k8s/apps/audit.yaml
kubectl apply -f k8s/apps/notification.yaml

kubectl rollout restart deployment/api-gateway        -n infra
kubectl rollout restart deployment/auth-service       -n apps
kubectl rollout restart deployment/pipeline-service   -n apps
kubectl rollout restart deployment/audit-log-service  -n apps
kubectl rollout restart deployment/notification-service -n apps

echo "================================================"
echo "STEP 4 — Wait for rollouts"
echo "================================================"

kubectl rollout status deployment/api-gateway        -n infra --timeout=120s
kubectl rollout status deployment/audit-log-service  -n apps  --timeout=120s
kubectl rollout status deployment/auth-service       -n apps  --timeout=120s
kubectl rollout status deployment/pipeline-service   -n apps  --timeout=120s

echo ""
echo "================================================"
echo "✅ ALL DONE — verify with:"
echo "  kubectl get pods -n apps"
echo "  kubectl get pods -n infra"
echo "================================================"
echo ""
echo "Then in your browser:"
echo "  1. Logout and login again → audit log entry created"
echo "  2. Go to Audit Logs page → should show USER_LOGIN entry"
echo "  3. Trigger a pipeline → notification appears in ~2min"

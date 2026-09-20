#!/bin/bash
# ============================================================
#  STB DevSecOps — Full K8s Bootstrap Script
#
#  Run this ONCE on the minikube / k8s node to set up:
#    1. NGINX Ingress controller
#    2. All namespaces (infra, monitoring, dev, staging, prod)
#    3. Secrets in all namespaces
#    4. Shared infra (MySQL, Eureka, Gateway)
#    5. Monitoring (Prometheus, Grafana)
#    6. Ingress rules
#
#  Usage:
#    chmod +x k8s/setup-all.sh
#    ./k8s/setup-all.sh
# ============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REGISTRY="192.168.56.10:5000"
NODE_IP="192.168.56.20"
DB_PASSWORD="${DB_PASSWORD:-change-me-local-database-password}"
JWT_SECRET="${JWT_SECRET:-dev-only-change-this-secret-key-which-is-at-least-32-characters}"
MAIL_USERNAME="${MAIL_USERNAME:-disabled@localhost}"
MAIL_PASSWORD="${MAIL_PASSWORD:-change-me-before-deployment}"

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║   STB DevSecOps — Kubernetes Bootstrap               ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

# ── Step 1: Enable NGINX Ingress on minikube ─────────────────
echo "► Step 1: Enabling NGINX Ingress controller..."
if command -v minikube &>/dev/null; then
  minikube addons enable ingress
  echo "  ✅ minikube ingress addon enabled"
else
  echo "  Installing NGINX Ingress (bare-metal)..."
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/baremetal/deploy.yaml
  kubectl wait --namespace ingress-nginx \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=180s
  echo "  ✅ NGINX Ingress installed"
fi

# ── Step 2: Create all namespaces ────────────────────────────
echo ""
echo "► Step 2: Creating namespaces..."
kubectl apply -f "${SCRIPT_DIR}/namespaces/namespaces.yaml"
echo "  ✅ Namespaces: infra, monitoring, dev, staging, prod"

# ── Step 3: Create secrets in all namespaces ─────────────────
echo ""
echo "► Step 3: Creating secrets..."

# db-secret (required before MySQL starts)
for NS in infra dev staging prod; do
  kubectl create secret generic db-secret \
    --from-literal=DB_USERNAME=root \
    --from-literal=DB_PASSWORD="$DB_PASSWORD" \
    -n "$NS" --dry-run=client -o yaml | kubectl apply -f -
done

# jwt-secret
for NS in infra apps dev staging prod; do
  kubectl create secret generic jwt-secret \
    --from-literal=JWT_SECRET="$JWT_SECRET" \
    -n "$NS" --dry-run=client -o yaml | kubectl apply -f -
done

# mail-secret
for NS in apps dev staging prod; do
  kubectl create secret generic mail-secret \
    --from-literal=MAIL_USERNAME="$MAIL_USERNAME" \
    --from-literal=MAIL_PASSWORD="$MAIL_PASSWORD" \
    -n "$NS" --dry-run=client -o yaml | kubectl apply -f -
done
echo "  ✅ Secrets applied to all namespaces"

# ── Step 4: Deploy shared infra ───────────────────────────────
echo ""
echo "► Step 4: Deploying shared infrastructure..."
kubectl apply -f "${SCRIPT_DIR}/infra/mysql.yaml"
kubectl apply -f "${SCRIPT_DIR}/infra/eureka.yaml"
kubectl apply -f "${SCRIPT_DIR}/infra/gateway.yaml"
echo "  Waiting for MySQL..."
kubectl rollout status deployment/mysql -n infra --timeout=180s || true
echo "  ✅ Shared infra deployed (infra namespace)"

# ── Step 5: Deploy monitoring ─────────────────────────────────
echo ""
echo "► Step 5: Deploying monitoring..."
kubectl apply -f "${SCRIPT_DIR}/monitoring/prometheus.yaml"
kubectl apply -f "${SCRIPT_DIR}/monitoring/grafana.yaml"
echo "  ✅ Monitoring deployed (monitoring namespace)"

# ── Step 6: Apply Ingress rules ───────────────────────────────
echo ""
echo "► Step 6: Applying Ingress rules..."
kubectl apply -f "${SCRIPT_DIR}/ingress/ingress-all.yaml"
echo "  ✅ Ingress rules applied"

# ── Step 7: /etc/hosts reminder ───────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║  Add to /etc/hosts (or Windows hosts file):          ║"
echo "║                                                       ║"
echo "║  ${NODE_IP}  dev.devsecops.local"
echo "║  ${NODE_IP}  staging.devsecops.local"
echo "║  ${NODE_IP}  devsecops.local"
echo "║                                                       ║"
echo "║  URLs:                                                ║"
echo "║    DEV     → http://dev.devsecops.local               ║"
echo "║    STAGING → http://staging.devsecops.local           ║"
echo "║    PROD    → http://devsecops.local                   ║"
echo "║                                                       ║"
echo "║  Git workflow:                                        ║"
echo "║    git push origin dev     → deploys to DEV           ║"
echo "║    git push origin staging → deploys to STAGING       ║"
echo "║    git push origin main    → gate → deploys to PROD   ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

# ── Step 8: Status ────────────────────────────────────────────
echo "► Current cluster state:"
kubectl get pods -n infra
kubectl get pods -n monitoring
echo ""
echo "✅ Bootstrap complete!"

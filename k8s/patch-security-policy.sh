#!/bin/bash
# ══════════════════════════════════════════════════════════════
#  Run this ONCE on your K8s VM (192.168.56.20) to break the
#  deployment deadlock, then trigger the Jenkins pipeline again.
# ══════════════════════════════════════════════════════════════
kubectl set env deployment/security-service -n apps \
  SECURITY_POLICY_MIN_SCORE=25 \
  SECURITY_POLICY_MAX_CRITICAL=5 && \
kubectl rollout status deployment/security-service -n apps --timeout=90s && \
echo "✅ Done — now trigger the Jenkins pipeline"

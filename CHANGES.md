# DevSecOps STB — Changes Summary

## Frontend Changes

### 1. Role-Based Privilege Display
- **shell.component.ts/html/scss** — Added role indicator badge in the topbar showing icon + label (👑 Admin, 💻 Dev, 🚀 DevOps, 🔍 Auditor) with role-colored styling
- **shell.component.scss** — User role in sidebar now color-coded per role
- **projects.component.ts/html/scss** — Added privilege banner on Projects page showing all permissions per role, with ✓/✗ indicators. DEV role sees "🔒 Déploiement réservé DevOps" instead of the deploy button

### 2. Rocket Launch Deploy Animation
- **Deploy-modal.component.html** — Full rocket launch scene during deployment step with animated rocket SVG, flame, exhaust smoke particles, and twinkling stars
- **Deploy-modal.component.scss** — Complete rewrite with modal-overlay/modal-panel CSS (was broken mismatch), plus rocket/flame/smoke/star animations

### 3. Bug Fixes
- **notifications.component.ts** — Extracted `load()` method; refresh button no longer calls lifecycle hook `ngOnInit()` directly
- **notifications.component.html** — `(click)="ngOnInit()"` → `(click)="load()"`
- **NotificationResponse.java** — Added missing `subject` field
- **NotificationServiceImpl.java** — Added `.subject(n.getSubject())` to `toResponse()` mapper

## Backend Changes

### 4. SMTP Configuration Fixed
- **Notification-Service/application.properties** — SMTP block now includes all required properties: `starttls.required=true`, connection/read/write timeouts (5000ms), proper Gmail port 587
- **k8s/secrets/mail-secret.yaml** (NEW) — Kubernetes Secret with base64-encoded Gmail credentials, ready to apply

### 5. Notification URL Fix
- **Pipeline-Service/application.properties** — Changed `notification.service.url` from `http://notification-service.apps.svc.cluster.local:8087` to `http://notification-service:8087` (simpler in-namespace DNS, avoids FQDN resolution failures)

## How to Apply

```bash
# 1. Apply the mail secret to your cluster
kubectl apply -f k8s/secrets/mail-secret.yaml

# 2. Rebuild and redeploy services that changed
./build-and-push.sh notification-service
./build-and-push.sh pipeline-service

# 3. Rebuild the frontend
cd frontend && npm install && ng build --configuration=production

# 4. Test notifications manually
curl -X POST http://<GATEWAY_IP>:30080/api/notifications/pipeline-event \
  -H "Content-Type: application/json" \
  -d '{"pipelineExecutionId":1,"projectId":1,"eventType":"PIPELINE_SUCCESS","projectName":"test","branch":"main","commitHash":"abc123","triggeredBy":"khaled"}'
```

#!/bin/sh
set -eu

NEXUS_URL="http://stb-nexus:8081"
PASSWORD="${NEXUS_ADMIN_PASSWORD:-admin-local}"

if curl -fsS -u "admin:${PASSWORD}" "${NEXUS_URL}/service/rest/v1/status" >/dev/null 2>&1; then
  echo "Nexus admin password is already configured."
else
  INITIAL_PASSWORD="$(cat /nexus-data/admin.password)"
  echo "Configuring the Nexus local admin account..."
  curl -fsS -u "admin:${INITIAL_PASSWORD}" -X PUT \
    -H 'Content-Type: text/plain' \
    --data-binary "${PASSWORD}" \
    "${NEXUS_URL}/service/rest/v1/security/users/admin/change-password"
fi

if curl -fsS -u "admin:${PASSWORD}" \
  "${NEXUS_URL}/service/rest/v1/repositories" | grep -q '"name" : "devsecops-artifacts"'; then
  echo "Nexus repository devsecops-artifacts already exists."
else
  echo "Creating Nexus raw repository devsecops-artifacts..."
  curl -fsS -u "admin:${PASSWORD}" -X POST \
    -H 'Content-Type: application/json' \
    "${NEXUS_URL}/service/rest/v1/repositories/raw/hosted" \
    --data-binary '{
      "name":"devsecops-artifacts",
      "online":true,
      "storage":{"blobStoreName":"default","strictContentTypeValidation":false,"writePolicy":"ALLOW"},
      "cleanup":null,
      "component":{"proprietaryComponents":false},
      "raw":{"contentDisposition":"ATTACHMENT"}
    }'
fi

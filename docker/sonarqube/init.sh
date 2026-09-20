#!/bin/sh
set -eu

SONAR_URL="http://stb-sonarqube:9000"
PASSWORD="${SONAR_ADMIN_PASSWORD:-admin-local}"

if curl -fsS -u "admin:${PASSWORD}" "${SONAR_URL}/api/authentication/validate" | grep -q '"valid":true'; then
  echo "SonarQube admin password is already configured."
else
  echo "Configuring the SonarQube local admin account..."
  curl -fsS -u admin:admin -X POST "${SONAR_URL}/api/users/change_password" \
    --data-urlencode "login=admin" \
    --data-urlencode "previousPassword=admin" \
    --data-urlencode "password=${PASSWORD}"
fi

# Tokens are displayed only once, so persist the generated value for Jenkins.
if [ ! -s /bootstrap/sonar-token ]; then
  curl -fsS -u "admin:${PASSWORD}" -X POST \
    "${SONAR_URL}/api/user_tokens/revoke" \
    --data-urlencode "name=jenkins-local" >/dev/null 2>&1 || true

  RESPONSE="$(curl -fsS -u "admin:${PASSWORD}" -X POST \
    "${SONAR_URL}/api/user_tokens/generate" \
    --data-urlencode "name=jenkins-local")"
  TOKEN="$(printf '%s' "$RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
  test -n "$TOKEN"
  printf '%s' "$TOKEN" > /bootstrap/sonar-token
  chmod 644 /bootstrap/sonar-token
  echo "SonarQube token generated for Jenkins."
fi

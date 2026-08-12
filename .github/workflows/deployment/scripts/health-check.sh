#!/usr/bin/env bash
set -euo pipefail

HEALTH_URL="${1:?Usage: health-check.sh <readiness-url>}"
MAX_RETRIES="${MAX_RETRIES:-12}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"

for attempt in $(seq 1 "$MAX_RETRIES"); do
  if curl --fail --silent --show-error "$HEALTH_URL" >/dev/null; then
    echo "Health check passed: $attempt/$MAX_RETRIES"
    exit 0
  fi
  sleep "$SLEEP_SECONDS"
done

# container_name을 사용하지 않으므로 Compose 서비스 기준으로 최근 로그를 남긴다.
ROOT="/home/ubuntu/puppyrun"
COMPOSE_PROJECT_NAME="${BACKEND_COMPOSE_PROJECT_NAME:-puppyrun}"
docker compose --project-name "$COMPOSE_PROJECT_NAME" \
  -f "$ROOT/compose/docker-compose.backend.yml" logs --tail 200 backend || true
echo "Health check failed: $HEALTH_URL"
exit 1

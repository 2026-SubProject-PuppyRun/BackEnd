#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
SERVICE_NAME="puppyrun-backend"
LIVENESS_URL="http://127.0.0.1:8081/actuator/health/liveness"
READINESS_URL="http://127.0.0.1:8081/actuator/health/readiness"
MAX_RETRIES=12
SLEEP_SEC=5

echo "===== Application Health Check ====="

for i in $(seq 1 "$MAX_RETRIES"); do
  if curl -fsS "$LIVENESS_URL" >/dev/null 2>&1 && curl -fsS "$READINESS_URL" >/dev/null 2>&1; then
    echo "[v] Application health check passed (attempt $i/$MAX_RETRIES)"
    exit 0
  fi

  echo "Waiting for backend application... (attempt $i/$MAX_RETRIES)"
  if [ "$i" -lt "$MAX_RETRIES" ]; then
    sleep "$SLEEP_SEC"
  fi
done

echo "===== Health Check Failed. Application Container Logs ($SERVICE_NAME) ====="
docker logs --tail 400 "$SERVICE_NAME" || true
exit 1

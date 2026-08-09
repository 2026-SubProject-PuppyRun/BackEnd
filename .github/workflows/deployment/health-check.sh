#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ENV_FILE="$SCRIPT_DIR/.env"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.deploy.yml"
SERVICE_NAME="puppyrun-backend"
LIVENESS_URL="http://127.0.0.1:8081/actuator/health/liveness"
READINESS_URL="http://127.0.0.1:8081/actuator/health/readiness"
MAX_RETRIES=10
SLEEP_SEC=10

echo "===== Deployment Validation ====="

if [ ! -f "$ENV_FILE" ]; then
  echo "[x] .env file not found: $ENV_FILE"
  exit 1
fi

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "[x] docker-compose file not found: $COMPOSE_FILE"
  exit 1
fi

for i in $(seq 1 "$MAX_RETRIES"); do
  if curl -fsS "$LIVENESS_URL" >/dev/null && curl -fsS "$READINESS_URL" >/dev/null; then
    echo "[v] Backend liveness and readiness checks passed (try $i/$MAX_RETRIES)"
    exit 0
  fi

  echo "Waiting for backend liveness/readiness... (try $i/$MAX_RETRIES)"
  if [ "$i" -lt "$MAX_RETRIES" ]; then
    sleep "$SLEEP_SEC"
  fi
done

echo "[x] Backend health check failed"
echo "===== Recent logs from docker container: $SERVICE_NAME ====="
docker logs --tail 200 "$SERVICE_NAME" || true
exit 1

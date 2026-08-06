#!/bin/bash

set -e

echo "===== Health Check ====="

# 서비스 이름 (systemd 기준)
SERVICE_NAME="puppyrun-backend"

# 헬스체크 대상 URL
HEALTH_URL="http://127.0.0.1:8081/actuator/health"

# 최대 재시도 횟수와 대기 시간
MAX_RETRIES=10
SLEEP_SEC=10

for i in $(seq 1 $MAX_RETRIES); do
  sleep $SLEEP_SEC
  if curl -sSf "$HEALTH_URL" >/dev/null; then
    echo "[v] Backend healthy (try $i/$MAX_RETRIES)"
    exit 0
  else
    echo "Waiting for backend... (try $i/$MAX_RETRIES)"
  fi
done


echo "[x] Backend health check failed"
echo "===== Recent logs from $SERVICE_NAME ====="
journalctl -u "$SERVICE_NAME" --no-pager -n 200
exit 1


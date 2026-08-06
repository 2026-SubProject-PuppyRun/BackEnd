#!/bin/bash

set -e

echo "===== Deployment Validation ====="


# =========================================
# 환경 설정 파일 존재 확인
# =========================================

ENV_FILE="/home/ubuntu/.env"
COMPOSE_FILE="/home/ubuntu/docker-compose.deploy.yml"

echo "===== Check Environment File ====="

if [ ! -f "$ENV_FILE" ]; then
  echo "[x] .env file not found: $ENV_FILE"
  exit 1
fi

echo "[v] .env file exists"


echo "===== Check Docker Compose File ====="

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "[x] docker-compose file not found: $COMPOSE_FILE"
  exit 1
fi

echo "[v] docker-compose file exists"



# =========================================
# Backend Health Check
# =========================================

echo "===== Health Check ====="


# Docker container name
SERVICE_NAME="puppyrun-backend"

# Health check URL
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


# =========================================
# 실패 처리
# =========================================

echo "[x] Backend health check failed"

echo "===== Recent logs from docker container: $SERVICE_NAME ====="

docker logs --tail 200 "$SERVICE_NAME" || true

exit 1

#!/usr/bin/env bash

set -euo pipefail

# 역할: deploy.sh와 rollback.sh가 backend 교체 뒤 호출하는 로컬 readiness 검증이다.
# 관리 포트는 Docker Compose에서 EC2 loopback에만 바인딩되어 외부에 노출되지 않는다.
LIVENESS_URL="http://127.0.0.1:8081/actuator/health/liveness"
READINESS_URL="http://127.0.0.1:8081/actuator/health/readiness"
MAX_RETRIES="${MAX_RETRIES:-12}"
SLEEP_SEC="${SLEEP_SEC:-5}"

# 애플리케이션 초기화 시간을 고려해 liveness와 readiness를 모두 재시도한다.
for attempt in $(seq 1 "$MAX_RETRIES"); do
  if curl -fsS "$LIVENESS_URL" >/dev/null 2>&1 && curl -fsS "$READINESS_URL" >/dev/null 2>&1; then
    echo "Application health check passed: $attempt/$MAX_RETRIES"
    exit 0
  fi
  sleep "$SLEEP_SEC"
done

# 최종 실패 시 SSM/GitHub Actions 로그에서 원인을 볼 수 있도록 최근 컨테이너 로그를 남긴다.
docker logs --tail 400 puppyrun-backend || true
echo "Application health check failed."
exit 1

#!/usr/bin/env bash
set -euo pipefail

HEALTH_URL="${1:?Usage: health-check.sh <readiness-url> [expected-image]}"
EXPECTED_IMAGE="${2:-}"
MAX_RETRIES="${MAX_RETRIES:-12}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"

for attempt in $(seq 1 "$MAX_RETRIES"); do
  if curl --fail --silent --show-error "$HEALTH_URL" >/dev/null; then
    echo "Health check passed: $attempt/$MAX_RETRIES"
    exit 0
  fi
  sleep "$SLEEP_SECONDS"
done

ROOT="/home/ubuntu/puppyrun"
COMPOSE_PROJECT_NAME="${BACKEND_COMPOSE_PROJECT_NAME:-puppyrun}"

# Compose 파일을 다시 해석하지 않고, backend 서비스로 생성된 컨테이너만 조회한다.
while IFS= read -r container_id; do
  [[ -z "$container_id" ]] && continue

  container_image=$(
    docker inspect --format '{{.Config.Image}}' "$container_id" 2>/dev/null || true
  )

  # 배포 대상 이미지 태그를 전달받았다면 해당 이미지로 실행된 컨테이너만 남긴다.
  if [[ -n "$EXPECTED_IMAGE" && "$container_image" != "$EXPECTED_IMAGE" ]]; then
    continue
  fi

  echo "Backend container logs (id=$container_id, image=$container_image)"
  docker logs --tail 500 "$container_id" || true
  break
done < <(
  docker ps -aq \
    --filter "label=com.docker.compose.project=$COMPOSE_PROJECT_NAME" \
    --filter "label=com.docker.compose.service=backend"
)

echo "Health check failed: $HEALTH_URL"
exit 1

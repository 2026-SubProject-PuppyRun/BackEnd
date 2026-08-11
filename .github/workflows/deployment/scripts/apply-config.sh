#!/usr/bin/env bash
set -euo pipefail

# 이미지 변경 없이 app.env/infra.env 변경을 적용한다.
# Compose 파일은 분리돼 있지만, 운영자는 이 명령 하나로 공통 설정을 반영할 수 있다.
ROOT=/home/ubuntu/puppyrun
CONFIG="$ROOT/config"
STATE="$ROOT/state"
BACKEND_COMPOSE="$ROOT/compose/docker-compose.backend.yml"
APP_ENV="$CONFIG/app.env"
LAST_SUCCESS="$CONFIG/app.env.last-success"

[[ -r "$APP_ENV" && -r "$CONFIG/deploy.env" && -f "$STATE/current-image" ]] || {
  echo "app.env, deploy.env, and current-image are required."; exit 1;
}
source "$CONFIG/deploy.env"
: "${BACKEND_COMPOSE_PROJECT_NAME:=puppyrun}"
: "${HEALTH_URL:=http://127.0.0.1:8081/actuator/health/readiness}"
CURRENT_IMAGE=$(<"$STATE/current-image")

restart_backend() {
  BACKEND_IMAGE="$CURRENT_IMAGE" docker compose --project-name "$BACKEND_COMPOSE_PROJECT_NAME" \
    --env-file "$APP_ENV" -f "$BACKEND_COMPOSE" up -d --force-recreate backend
  "$ROOT/scripts/health-check.sh" "$HEALTH_URL"
}

restore_last_success() {
  [[ -f "$LAST_SUCCESS" ]] || return 1
  echo "Configuration apply failed; restoring the last successful app.env."
  cp "$LAST_SUCCESS" "$APP_ENV"
  "$ROOT/scripts/infra.sh" up
  restart_backend
}

docker compose --project-name "$BACKEND_COMPOSE_PROJECT_NAME" \
  --env-file "$APP_ENV" -f "$BACKEND_COMPOSE" config -q

if ! "$ROOT/scripts/infra.sh" up || ! restart_backend; then
  restore_last_success || true
  exit 1
fi

install -m 640 "$APP_ENV" "$LAST_SUCCESS"
echo "Configuration applied successfully."

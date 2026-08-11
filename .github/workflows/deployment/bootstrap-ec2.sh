#!/usr/bin/env bash
set -euo pipefail

# EC2에 backend/infra 배포 정의를 설치한다. config/의 운영값은 덮어쓰지 않는다.
[[ "${EUID}" -eq 0 ]] || { echo "Run with sudo."; exit 1; }
SOURCE_DIRECTORY=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
SCRIPTS_SOURCE="$SOURCE_DIRECTORY/scripts"
COMPOSE_SOURCE="$SOURCE_DIRECTORY/compose"
ENV_SOURCE="$SOURCE_DIRECTORY/env"
ROOT=/home/ubuntu/puppyrun

install -d -o root -g ubuntu -m 750 \
  "$ROOT/scripts" "$ROOT/compose" "$ROOT/config" "$ROOT/state" "$ROOT/logs" "$ROOT/locks"
install -m 755 "$SCRIPTS_SOURCE/deploy.sh" "$ROOT/scripts/deploy.sh"
install -m 755 "$SCRIPTS_SOURCE/rollback.sh" "$ROOT/scripts/rollback.sh"
install -m 755 "$SCRIPTS_SOURCE/health-check.sh" "$ROOT/scripts/health-check.sh"
install -m 755 "$SCRIPTS_SOURCE/cleanup-images.sh" "$ROOT/scripts/cleanup-images.sh"
install -m 755 "$SCRIPTS_SOURCE/infra.sh" "$ROOT/scripts/infra.sh"
install -m 755 "$SCRIPTS_SOURCE/apply-config.sh" "$ROOT/scripts/apply-config.sh"
install -m 640 "$COMPOSE_SOURCE/docker-compose.backend.yml" "$ROOT/compose/docker-compose.backend.yml"
install -m 640 "$COMPOSE_SOURCE/docker-compose.infra.yml" "$ROOT/compose/docker-compose.infra.yml"
install -m 640 "$COMPOSE_SOURCE/config.alloy" "$ROOT/compose/config.alloy"

# Compose가 분리되어도 서비스 이름으로 통신하도록 공용 네트워크를 준비한다.
docker network inspect puppyrun-app >/dev/null 2>&1 || docker network create puppyrun-app >/dev/null
docker network inspect puppyrun-observability >/dev/null 2>&1 || docker network create puppyrun-observability >/dev/null

if [[ ! -f "$ROOT/config/deploy.env" ]]; then
  install -m 640 "$ENV_SOURCE/deploy.env.example" "$ROOT/config/deploy.env"
  echo "Created deploy.env. Update its AWS_ACCOUNT_ID before deployment."
fi
if [[ ! -f "$ROOT/config/app.env" ]]; then
  install -m 640 "$ENV_SOURCE/app.env.example" "$ROOT/config/app.env"
  echo "Created app.env. Replace every CHANGE_ME value before deployment."
fi
if [[ ! -f "$ROOT/config/infra.env" ]]; then
  install -m 640 "$ENV_SOURCE/infra.env.example" "$ROOT/config/infra.env"
  echo "Created infra.env. Replace every CHANGE_ME value before starting Alloy."
fi

echo "Bootstrap completed: $ROOT"

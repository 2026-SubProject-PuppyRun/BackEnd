#!/usr/bin/env bash
set -euo pipefail

# EC2 운영 디렉터리와 공유 네트워크만 초기화한다.
[[ "${EUID}" -eq 0 ]] || { echo "Run with sudo."; exit 1; }
SOURCE_DIRECTORY=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
SCRIPTS_SOURCE="$SOURCE_DIRECTORY/scripts"
COMPOSE_SOURCE="$SOURCE_DIRECTORY/compose"
ROOT="${DEPLOY_ROOT:-puppyrun}"

install -d -o root -g ubuntu -m 750 \
  "$ROOT/scripts" "$ROOT/compose" "$ROOT/config" "$ROOT/state" "$ROOT/logs" "$ROOT/locks"

# config의 운영 환경파일은 복사하지 않고, 배포 정의만 설치한다.
cp -a "$SCRIPTS_SOURCE/." "$ROOT/scripts/"
cp -a "$COMPOSE_SOURCE/." "$ROOT/compose/"

# Compose가 분리되어도 서비스 이름으로 통신하도록 공용 네트워크를 준비한다.
docker network inspect puppyrun-app >/dev/null 2>&1 || docker network create puppyrun-app >/dev/null
docker network inspect puppyrun-observability >/dev/null 2>&1 || docker network create puppyrun-observability >/dev/null

echo "Bootstrap completed: $ROOT"

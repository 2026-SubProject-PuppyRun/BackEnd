#!/usr/bin/env bash
set -euo pipefail

# EC2 운영 디렉터리와 Git 작업본만 초기화한다. 배포 파일은 GitHub Actions가 동기화한다.
[[ "${EUID}" -eq 0 ]] || { echo "Run with sudo."; exit 1; }
SOURCE_DIRECTORY=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPOSITORY_ROOT=$(cd -- "$SOURCE_DIRECTORY/../../.." && pwd)
REPOSITORY_URL=$(git -C "$REPOSITORY_ROOT" remote get-url origin)
ROOT="${DEPLOY_ROOT:-puppyrun}"

install -d -o root -g ubuntu -m 750 \
  "$ROOT/scripts" "$ROOT/compose" "$ROOT/config" "$ROOT/state" "$ROOT/logs" "$ROOT/locks"

if [[ ! -d "$ROOT/.git" ]]; then
  git -C "$ROOT" init
  git -C "$ROOT" remote add origin "$REPOSITORY_URL"
fi

# Compose가 분리되어도 서비스 이름으로 통신하도록 공용 네트워크를 준비한다.
docker network inspect puppyrun-app >/dev/null 2>&1 || docker network create puppyrun-app >/dev/null
docker network inspect puppyrun-observability >/dev/null 2>&1 || docker network create puppyrun-observability >/dev/null

echo "Bootstrap completed: $ROOT. Run the Sync deployment configuration to EC2 workflow."

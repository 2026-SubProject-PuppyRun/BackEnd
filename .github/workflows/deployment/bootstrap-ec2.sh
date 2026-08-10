#!/usr/bin/env bash
set -euo pipefail

# EC2에서 한 번 실행해 고정 배포 파일을 설치한다. 일반 CI/CD에서는 다시 실행하지 않는다.
[[ "${EUID}" -eq 0 ]] || { echo "Run with sudo."; exit 1; }
SOURCE_DIRECTORY=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ROOT=/home/ubuntu/puppyrun

install -d -o root -g ubuntu -m 750 \
  "$ROOT/scripts" "$ROOT/compose" "$ROOT/config" "$ROOT/state" "$ROOT/logs" "$ROOT/locks"
install -m 755 "$SOURCE_DIRECTORY/deploy.sh" "$ROOT/scripts/deploy.sh"
install -m 755 "$SOURCE_DIRECTORY/rollback.sh" "$ROOT/scripts/rollback.sh"
install -m 755 "$SOURCE_DIRECTORY/health-check.sh" "$ROOT/scripts/health-check.sh"
install -m 755 "$SOURCE_DIRECTORY/cleanup-images.sh" "$ROOT/scripts/cleanup-images.sh"
install -m 640 "$SOURCE_DIRECTORY/docker-compose.deploy.yml" "$ROOT/compose/docker-compose.yml"
install -m 600 "$SOURCE_DIRECTORY/config/firebase-service-account.json"

if [[ ! -f "$ROOT/config/deploy.env" ]]; then
  install -m 640 "$SOURCE_DIRECTORY/deploy.env.example" "$ROOT/config/deploy.env"
  echo "Created deploy.env. Update its AWS_ACCOUNT_ID before deployment."
fi
if [[ ! -f "$ROOT/config/app.env" ]]; then
  install -m 600 /dev/null "$ROOT/config/app.env"
  echo "Created empty app.env. Add application secrets before deployment."
fi

echo "Bootstrap completed: $ROOT"

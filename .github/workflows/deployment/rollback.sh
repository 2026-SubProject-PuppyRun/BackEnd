#!/usr/bin/env bash

set -euo pipefail

# 역할: Grafana Cloud 알림이 Lambda/SSM을 통해 호출하는 런타임 롤백이다.
# 흐름: previous의 digest·.env·Compose로 기동 → health check → current/previous 링크 교환.
# 실패: previous 후보가 건강하지 않으면 기존 current를 다시 기동하고 링크는 유지한다.
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIRECTORY="${ROOT_DIRECTORY:-/home/ubuntu/puppyrun}"
CURRENT_LINK="$ROOT_DIRECTORY/current"
PREVIOUS_LINK="$ROOT_DIRECTORY/previous"

: "${AWS_REGION:?AWS_REGION is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"

# 두 성공 릴리스가 모두 있어야만 자동 롤백을 허용한다.
if [ "$ROOT_DIRECTORY" != "/home/ubuntu/puppyrun" ] || [ ! -L "$CURRENT_LINK" ] || [ ! -L "$PREVIOUS_LINK" ]; then
  echo "Current or previous release is not registered. Rollback is unavailable."
  exit 1
fi

CURRENT_RELEASE=$(readlink -f "$CURRENT_LINK")
PREVIOUS_RELEASE=$(readlink -f "$PREVIOUS_LINK")
PREVIOUS_IMAGE=$(sed -n 's/^BACKEND_IMAGE=//p' "$PREVIOUS_RELEASE/metadata.env")
for required_file in "$PREVIOUS_RELEASE/.env" "$PREVIOUS_RELEASE/docker-compose.deploy.yml" "$PREVIOUS_RELEASE/health-check.sh"; do
  if [ ! -f "$required_file" ]; then
    echo "Rollback release is incomplete: $required_file"
    exit 1
  fi
done
if [ -z "$PREVIOUS_IMAGE" ]; then
  echo "BACKEND_IMAGE is missing from $PREVIOUS_RELEASE/metadata.env"
  exit 1
fi

# previous 릴리스에 기록된 immutable digest를 다시 pull한 뒤, 그 릴리스의 환경과 Compose로 기동한다.
aws ecr get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
docker pull "$PREVIOUS_IMAGE"
BACKEND_IMAGE="$PREVIOUS_IMAGE" docker compose \
  -p puppyrun \
  --env-file "$PREVIOUS_RELEASE/.env" \
  -f "$PREVIOUS_RELEASE/docker-compose.deploy.yml" \
  up -d --force-recreate backend

# 롤백 후보가 실패하면 현재 릴리스를 재기동한다. 포인터 교환은 이 이후에만 일어난다.
if ! bash "$PREVIOUS_RELEASE/health-check.sh"; then
  CURRENT_IMAGE=$(sed -n 's/^BACKEND_IMAGE=//p' "$CURRENT_RELEASE/metadata.env")
  BACKEND_IMAGE="$CURRENT_IMAGE" docker compose \
    -p puppyrun \
    --env-file "$CURRENT_RELEASE/.env" \
    -f "$CURRENT_RELEASE/docker-compose.deploy.yml" \
    up -d --force-recreate backend || true
  bash "$CURRENT_RELEASE/health-check.sh" || true
  exit 1
fi

# 정상 검증 후에만 current와 previous를 원자적으로 교환한다.
ln -sfn "$CURRENT_RELEASE" "$PREVIOUS_LINK.next"
ln -sfn "$PREVIOUS_RELEASE" "$CURRENT_LINK.next"
mv -Tf "$PREVIOUS_LINK.next" "$PREVIOUS_LINK"
mv -Tf "$CURRENT_LINK.next" "$CURRENT_LINK"

echo "Rollback succeeded: $(basename "$PREVIOUS_RELEASE")"

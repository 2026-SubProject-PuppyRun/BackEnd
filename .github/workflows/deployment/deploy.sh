#!/usr/bin/env bash

set -euo pipefail

# 역할: EC2 releases/<release-tag> 후보 디렉터리에서 실행되어 새 이미지를 기동한다.
# 흐름: 후보 구성 검증 → digest 고정 → backend 교체 → health check → current/previous 승격.
# 실패: current 릴리스의 이미지·.env·Compose로 즉시 복구하고 링크는 변경하지 않는다.
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIRECTORY="${ROOT_DIRECTORY:-/home/ubuntu/puppyrun}"
RELEASES_DIRECTORY="$ROOT_DIRECTORY/releases"
NEW_LINK="$ROOT_DIRECTORY/new"
CURRENT_LINK="$ROOT_DIRECTORY/current"
PREVIOUS_LINK="$ROOT_DIRECTORY/previous"
ENV_FILE="$SCRIPT_DIR/.env"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.deploy.yml"
HEALTH_CHECK_SCRIPT="$SCRIPT_DIR/health-check.sh"
METADATA_FILE="$SCRIPT_DIR/metadata.env"
SERVICE_NAME="backend"

# GitHub Actions/SSM이 전달한 후보 릴리스 식별자와 AWS/ECR 연결 정보다.
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${AWS_REGION:?AWS_REGION is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"

IMAGE_TAG="${1:-${DEPLOY_IMAGE_TAG:-}}"
if [ -z "$IMAGE_TAG" ]; then
  echo "Usage: RELEASE_TAG=... AWS_REGION=... AWS_ACCOUNT_ID=... $0 <immutable-image-tag>"
  exit 2
fi

# 예상한 EC2 위치와 new 링크만 허용해 잘못된 경로에서의 배포·삭제를 방지한다.
if [ "$ROOT_DIRECTORY" != "/home/ubuntu/puppyrun" ] || [ "$SCRIPT_DIR" != "$RELEASES_DIRECTORY/$RELEASE_TAG" ]; then
  echo "Unexpected deployment directory."
  exit 1
fi
if [ ! -L "$NEW_LINK" ] || [ "$(readlink -f "$NEW_LINK")" != "$SCRIPT_DIR" ]; then
  echo "New release link does not point to this candidate."
  exit 1
fi
for required_file in "$ENV_FILE" "$COMPOSE_FILE" "$HEALTH_CHECK_SCRIPT"; do
  if [ ! -f "$required_file" ]; then
    echo "Candidate release is incomplete: $required_file"
    exit 1
  fi
done

ECR_REPOSITORY="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/puppyrun-backend"
REQUESTED_IMAGE="$ECR_REPOSITORY:$IMAGE_TAG"

restore_current_release() {
  # 후보 기동 또는 health check 실패 시, 포인터를 건드리지 않고 기존 current를 재기동한다.
  if [ ! -L "$CURRENT_LINK" ]; then
    echo "No current release is registered; restoration is unavailable."
    return 1
  fi

  local current_release current_image
  current_release=$(readlink -f "$CURRENT_LINK")
  current_image=$(sed -n 's/^BACKEND_IMAGE=//p' "$current_release/metadata.env")
  if [ -z "$current_image" ] || [ ! -f "$current_release/.env" ] || [ ! -f "$current_release/docker-compose.deploy.yml" ] || [ ! -f "$current_release/health-check.sh" ]; then
    echo "Current release is incomplete; restoration is unavailable."
    return 1
  fi

  docker pull "$current_image"
  BACKEND_IMAGE="$current_image" docker compose \
    -p puppyrun \
    --env-file "$current_release/.env" \
    -f "$current_release/docker-compose.deploy.yml" \
    up -d --force-recreate "$SERVICE_NAME"
  bash "$current_release/health-check.sh"
}

activate_candidate() {
  # 후보 검증이 끝난 뒤에만 previous ← current, current ← candidate 순으로 원자적으로 갱신한다.
  local old_previous=""
  if [ -L "$PREVIOUS_LINK" ]; then
    old_previous=$(readlink -f "$PREVIOUS_LINK")
  fi
  if [ -L "$CURRENT_LINK" ]; then
    ln -sfn "$(readlink -f "$CURRENT_LINK")" "$PREVIOUS_LINK.next"
    mv -Tf "$PREVIOUS_LINK.next" "$PREVIOUS_LINK"
  fi
  ln -sfn "$SCRIPT_DIR" "$CURRENT_LINK.next"
  mv -Tf "$CURRENT_LINK.next" "$CURRENT_LINK"

  # current/previous 이외의 과거 성공 릴리스 하나만 정리한다.
  if [ -n "$old_previous" ] && [ "$old_previous" != "$SCRIPT_DIR" ]; then
    rm -rf -- "$old_previous"
  fi
}

# SSM이 이미 pull한 경우는 중복 다운로드를 건너뛰고, 수동 실행은 직접 ECR에서 pull한다.
if [ "${SKIP_IMAGE_PULL:-false}" != "true" ]; then
  aws ecr get-login-password --region "$AWS_REGION" |
    docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
  docker pull "$REQUESTED_IMAGE"
fi

# 태그 대신 실제 RepoDigest를 기록·실행해 이후 태그 값이 바뀌어도 같은 이미지를 보장한다.
BACKEND_IMAGE=$(docker image inspect --format '{{index .RepoDigests 0}}' "$REQUESTED_IMAGE")
if [ -z "$BACKEND_IMAGE" ] || [ "$BACKEND_IMAGE" = "<no value>" ]; then
  echo "Unable to resolve ECR digest for: $REQUESTED_IMAGE"
  exit 1
fi

# 후보의 이미지·환경·Compose 체크섬을 남긴다. rollback은 이 릴리스 디렉터리를 그대로 사용한다.
IMAGE_DIGEST="${BACKEND_IMAGE##*@}"
{
  printf 'RELEASE_TAG=%s\n' "$RELEASE_TAG"
  printf 'REQUESTED_IMAGE=%s\n' "$REQUESTED_IMAGE"
  printf 'BACKEND_IMAGE=%s\n' "$BACKEND_IMAGE"
  printf 'IMAGE_DIGEST=%s\n' "$IMAGE_DIGEST"
  printf 'DEPLOYED_AT_UTC=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf 'ENV_SHA256=%s\n' "$(sha256sum "$ENV_FILE" | awk '{print $1}')"
  printf 'COMPOSE_SHA256=%s\n' "$(sha256sum "$COMPOSE_FILE" | awk '{print $1}')"
  printf 'STATUS=candidate\n'
} > "$METADATA_FILE"
chmod 600 "$METADATA_FILE" "$ENV_FILE"

# 기존 backend를 새 후보로 교체 기동한다. Compose의 다른 서비스·볼륨은 내리지 않는다.
if ! BACKEND_IMAGE="$BACKEND_IMAGE" docker compose \
  -p puppyrun \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  up -d --force-recreate "$SERVICE_NAME"; then
  printf 'STATUS=failed-to-start\n' >> "$METADATA_FILE"
  restore_current_release || true
  exit 1
fi

# liveness와 readiness가 모두 통과해야만 후보를 성공 릴리스로 승격한다.
if ! bash "$HEALTH_CHECK_SCRIPT"; then
  printf 'STATUS=failed-health-check\n' >> "$METADATA_FILE"
  restore_current_release || true
  exit 1
fi

printf 'STATUS=active\n' >> "$METADATA_FILE"
activate_candidate
echo "Deployment succeeded: $RELEASE_TAG"

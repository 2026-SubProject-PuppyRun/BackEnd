#!/usr/bin/env bash
set -euo pipefail

ROOT="/home/ubuntu/puppyrun"
CONFIG="$ROOT/config"
STATE="$ROOT/state"
LOGS="$ROOT/logs"
LOCK="$ROOT/locks/deploy.lock"
COMPOSE="$ROOT/compose/docker-compose.backend.yml"
APP_ENV="$CONFIG/app.env"
DEPLOY_ENV="$CONFIG/deploy.env"
HEALTH_CHECK="$ROOT/scripts/health-check.sh"
CLEANUP_SCRIPT="$ROOT/scripts/cleanup-images.sh"
RUNTIME_REPOSITORY=puppyrun-runtime
CURRENT_TAG="$RUNTIME_REPOSITORY:current"
PREVIOUS_TAG="$RUNTIME_REPOSITORY:previous"
CANDIDATE_TAG="$RUNTIME_REPOSITORY:candidate"

IMAGE_TAG="${1:?Usage: deploy.sh <immutable-image-tag>}"
[[ "$IMAGE_TAG" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] || { echo "Invalid image tag."; exit 2; }
[[ -r "$APP_ENV" && -r "$DEPLOY_ENV" ]] || { echo "Missing EC2 config files."; exit 1; }

source "$DEPLOY_ENV"
: "${AWS_REGION:?AWS_REGION is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY is required}"
: "${BACKEND_COMPOSE_PROJECT_NAME:=puppyrun}"
: "${HEALTH_URL:=http://127.0.0.1:8081/actuator/health/readiness}"

mkdir -p "$STATE" "$LOGS" "$(dirname "$LOCK")"
umask 027
LOG_FILE="$LOGS/deploy-$(date -u +%Y%m%dT%H%M%SZ)-$IMAGE_TAG.log"
ln -sfn "$(basename "$LOG_FILE")" "$LOGS/latest-deploy.log"
exec >> "$LOG_FILE" 2>&1
exec 9> "$LOCK"
flock -n 9 || { echo "Another deployment is already running."; exit 1; }

ECR_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"
CURRENT_IMAGE=""
[[ -f "$STATE/current-image" ]] && CURRENT_IMAGE=$(<"$STATE/current-image")
CURRENT_VERSION="unknown"
[[ -f "$STATE/current-version" ]] && CURRENT_VERSION=$(<"$STATE/current-version")

write_state() {
  local name="$1" value="$2" temporary
  temporary=$(mktemp "$STATE/.${name}.XXXXXX")
  printf '%s\n' "$value" > "$temporary"
  mv "$temporary" "$STATE/$name"
}

run_image() {
  local image_reference="$1" app_version="${2:-unknown}"
  BACKEND_IMAGE="$image_reference" APP_VERSION="$app_version" docker compose --project-name "$BACKEND_COMPOSE_PROJECT_NAME" \
    --env-file "$APP_ENV" -f "$COMPOSE" up -d --no-deps --force-recreate backend
}

ensure_local_tag() {
  local local_tag="$1" image_reference="$2"
  docker image inspect "$local_tag" >/dev/null 2>&1 && return 0
  docker image inspect "$image_reference" >/dev/null 2>&1 || return 1
  docker tag "$image_reference" "$local_tag"
}

restore_current() {
  ensure_local_tag "$CURRENT_TAG" "$CURRENT_IMAGE" || {
    echo "No local current image is available for restoration."
    return 1
  }
  echo "Restoring local image: $CURRENT_TAG"
  run_image "$CURRENT_TAG" "$CURRENT_VERSION"
  "$HEALTH_CHECK" "$HEALTH_URL"
}

promote_candidate() {
  # 현재 성공 버전은 previous로 남기고, 검증된 candidate만 current로 승격한다.
  if docker image inspect "$CURRENT_TAG" >/dev/null 2>&1; then
    docker image rm -f "$PREVIOUS_TAG" >/dev/null 2>&1 || true
    docker tag "$CURRENT_TAG" "$PREVIOUS_TAG"
  fi
  docker image rm -f "$CURRENT_TAG" >/dev/null 2>&1 || true
  docker tag "$CANDIDATE_TAG" "$CURRENT_TAG"
  docker image rm -f "$CANDIDATE_TAG" >/dev/null 2>&1 || true
}

echo "Deploy request: $IMAGE_TAG"
aws ecr get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
docker pull "$ECR_URI:$IMAGE_TAG"
CANDIDATE_IMAGE=$(docker image inspect --format '{{index .RepoDigests 0}}' "$ECR_URI:$IMAGE_TAG")
[[ -n "$CANDIDATE_IMAGE" && "$CANDIDATE_IMAGE" != "<no value>" ]] || { echo "Cannot resolve image digest."; exit 1; }
docker image rm -f "$CANDIDATE_TAG" >/dev/null 2>&1 || true
docker tag "$CANDIDATE_IMAGE" "$CANDIDATE_TAG"

if ! run_image "$CANDIDATE_TAG" "$IMAGE_TAG" || ! "$HEALTH_CHECK" "$HEALTH_URL"; then
  echo "Candidate failed; restoring current image."
  restore_current || true
  docker image rm -f "$CANDIDATE_TAG" >/dev/null 2>&1 || true
  exit 1
fi

promote_candidate
if [[ -n "$CURRENT_IMAGE" ]]; then
  write_state previous-image "$CURRENT_IMAGE"
  write_state previous-version "$CURRENT_VERSION"
fi
write_state current-image "$CANDIDATE_IMAGE"
write_state current-version "$IMAGE_TAG"
# 이후 app.env만 변경했을 때 실패하면 이 시점의 정상 설정으로 복구할 수 있다.
install -m 640 "$APP_ENV" "$CONFIG/app.env.last-success"
"$CLEANUP_SCRIPT" || echo "Image cleanup failed; deployment remains successful."
echo "Deployment succeeded: $CANDIDATE_IMAGE"

#!/usr/bin/env bash
set -euo pipefail

ROOT=/home/ubuntu/puppyrun
CONFIG="$ROOT/config"
STATE="$ROOT/state"
LOGS="$ROOT/logs"
LOCK="$ROOT/locks/deploy.lock"
COMPOSE="$ROOT/compose/docker-compose.yml"
APP_ENV="$CONFIG/app.env"
DEPLOY_ENV="$CONFIG/deploy.env"
HEALTH_CHECK="$ROOT/scripts/health-check.sh"

IMAGE_TAG="${1:?Usage: deploy.sh <immutable-image-tag>}"
[[ "$IMAGE_TAG" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]] || { echo "Invalid image tag."; exit 2; }
[[ -r "$APP_ENV" && -r "$DEPLOY_ENV" ]] || { echo "Missing EC2 config files."; exit 1; }

source "$DEPLOY_ENV"
: "${AWS_REGION:?AWS_REGION is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY is required}"
: "${COMPOSE_PROJECT_NAME:=puppyrun}"
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

write_state() {
  local name="$1" value="$2" temporary
  temporary=$(mktemp "$STATE/.${name}.XXXXXX")
  printf '%s\n' "$value" > "$temporary"
  mv "$temporary" "$STATE/$name"
}

run_image() {
  BACKEND_IMAGE="$1" docker compose --project-name "$COMPOSE_PROJECT_NAME" \
    --env-file "$APP_ENV" -f "$COMPOSE" up -d --force-recreate backend
}

restore_current() {
  [[ -n "$CURRENT_IMAGE" ]] || return 1
  echo "Restoring $CURRENT_IMAGE"
  run_image "$CURRENT_IMAGE"
  "$HEALTH_CHECK" "$HEALTH_URL"
}

echo "Deploy request: $IMAGE_TAG"
aws ecr get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
docker pull "$ECR_URI:$IMAGE_TAG"
CANDIDATE_IMAGE=$(docker image inspect --format '{{index .RepoDigests 0}}' "$ECR_URI:$IMAGE_TAG")
[[ -n "$CANDIDATE_IMAGE" && "$CANDIDATE_IMAGE" != "<no value>" ]] || { echo "Cannot resolve image digest."; exit 1; }

if ! run_image "$CANDIDATE_IMAGE" || ! "$HEALTH_CHECK" "$HEALTH_URL"; then
  echo "Candidate failed; restoring current image."
  restore_current || true
  exit 1
fi

[[ -n "$CURRENT_IMAGE" ]] && write_state previous-image "$CURRENT_IMAGE"
write_state current-image "$CANDIDATE_IMAGE"
echo "Deployment succeeded: $CANDIDATE_IMAGE"

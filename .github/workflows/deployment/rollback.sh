#!/usr/bin/env bash
set -euo pipefail

ROOT=/home/ubuntu/puppyrun
CONFIG="$ROOT/config"
STATE="$ROOT/state"
COMPOSE="$ROOT/compose/docker-compose.yml"
source "$CONFIG/deploy.env"
: "${COMPOSE_PROJECT_NAME:=puppyrun}"
: "${HEALTH_URL:=http://127.0.0.1:8081/actuator/health/readiness}"

[[ -f "$STATE/current-image" && -f "$STATE/previous-image" ]] || { echo "Rollback state is unavailable."; exit 1; }
CURRENT_IMAGE=$(<"$STATE/current-image")
PREVIOUS_IMAGE=$(<"$STATE/previous-image")

aws ecr get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
docker pull "$PREVIOUS_IMAGE"

BACKEND_IMAGE="$PREVIOUS_IMAGE" docker compose --project-name "$COMPOSE_PROJECT_NAME" \
  --env-file "$CONFIG/app.env" -f "$COMPOSE" up -d --force-recreate backend

if ! "$ROOT/scripts/health-check.sh" "$HEALTH_URL"; then
  BACKEND_IMAGE="$CURRENT_IMAGE" docker compose --project-name "$COMPOSE_PROJECT_NAME" \
    --env-file "$CONFIG/app.env" -f "$COMPOSE" up -d --force-recreate backend || true
  exit 1
fi

printf '%s\n' "$PREVIOUS_IMAGE" > "$STATE/current-image"
printf '%s\n' "$CURRENT_IMAGE" > "$STATE/previous-image"
echo "Rollback succeeded: $PREVIOUS_IMAGE"

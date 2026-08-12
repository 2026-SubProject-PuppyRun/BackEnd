#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIRECTORY=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ROOT=$(cd -- "$SCRIPT_DIRECTORY/.." && pwd)
CONFIG="$ROOT/config"
STATE="$ROOT/state"
COMPOSE="$ROOT/compose/docker-compose.backend.yml"
RUNTIME_REPOSITORY=puppyrun-runtime
CURRENT_TAG="$RUNTIME_REPOSITORY:current"
PREVIOUS_TAG="$RUNTIME_REPOSITORY:previous"
source "$CONFIG/deploy.env"
: "${BACKEND_COMPOSE_PROJECT_NAME:=puppyrun}"
: "${HEALTH_URL:=http://127.0.0.1:8081/actuator/health/readiness}"

[[ -f "$STATE/current-image" && -f "$STATE/previous-image" ]] || { echo "Rollback state is unavailable."; exit 1; }
CURRENT_IMAGE=$(<"$STATE/current-image")
PREVIOUS_IMAGE=$(<"$STATE/previous-image")
CURRENT_VERSION="unknown"
PREVIOUS_VERSION="unknown"
[[ -f "$STATE/current-version" ]] && CURRENT_VERSION=$(<"$STATE/current-version")
[[ -f "$STATE/previous-version" ]] && PREVIOUS_VERSION=$(<"$STATE/previous-version")

ensure_local_tag() {
  local local_tag="$1" image_reference="$2"
  docker image inspect "$local_tag" >/dev/null 2>&1 && return 0
  docker image inspect "$image_reference" >/dev/null 2>&1 || return 1
  docker tag "$image_reference" "$local_tag"
}

ensure_local_tag "$CURRENT_TAG" "$CURRENT_IMAGE" || { echo "Local current image is unavailable."; exit 1; }
ensure_local_tag "$PREVIOUS_TAG" "$PREVIOUS_IMAGE" || { echo "Local previous image is unavailable."; exit 1; }

CURRENT_ID=$(docker image inspect --format '{{.Id}}' "$CURRENT_TAG")
PREVIOUS_ID=$(docker image inspect --format '{{.Id}}' "$PREVIOUS_TAG")
SWAP_CURRENT_TAG="$RUNTIME_REPOSITORY:swap-current"
SWAP_PREVIOUS_TAG="$RUNTIME_REPOSITORY:swap-previous"

BACKEND_IMAGE="$PREVIOUS_TAG" APP_VERSION="$PREVIOUS_VERSION" docker compose --project-name "$BACKEND_COMPOSE_PROJECT_NAME" \
  --env-file "$CONFIG/app.env" -f "$COMPOSE" up -d --no-deps --force-recreate backend

if ! "$ROOT/scripts/health-check.sh" "$HEALTH_URL"; then
  BACKEND_IMAGE="$CURRENT_TAG" APP_VERSION="$CURRENT_VERSION" docker compose --project-name "$BACKEND_COMPOSE_PROJECT_NAME" \
    --env-file "$CONFIG/app.env" -f "$COMPOSE" up -d --no-deps --force-recreate backend || true
  exit 1
fi

# 현재 backend는 previous image로 이미 교체됐다. 임시 tag를 거쳐 두 local tag를 교환한다.
# -f는 tag만 제거하며, 직전에 만든 swap tag가 image ID를 계속 보존한다.
docker image rm -f "$SWAP_CURRENT_TAG" "$SWAP_PREVIOUS_TAG" >/dev/null 2>&1 || true
docker tag "$CURRENT_ID" "$SWAP_CURRENT_TAG"
docker tag "$PREVIOUS_ID" "$SWAP_PREVIOUS_TAG"
docker image rm -f "$CURRENT_TAG" "$PREVIOUS_TAG" >/dev/null 2>&1 || true
docker tag "$SWAP_PREVIOUS_TAG" "$CURRENT_TAG"
docker tag "$SWAP_CURRENT_TAG" "$PREVIOUS_TAG"
docker image rm -f "$SWAP_CURRENT_TAG" "$SWAP_PREVIOUS_TAG" >/dev/null 2>&1 || true
printf '%s\n' "$PREVIOUS_IMAGE" > "$STATE/current-image"
printf '%s\n' "$CURRENT_IMAGE" > "$STATE/previous-image"
printf '%s\n' "$PREVIOUS_VERSION" > "$STATE/current-version"
printf '%s\n' "$CURRENT_VERSION" > "$STATE/previous-version"
echo "Rollback succeeded: $PREVIOUS_IMAGE"

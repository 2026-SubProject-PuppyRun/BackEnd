#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
SERVICE_NAME="backend"
RELEASES_DIRECTORY="/home/ubuntu/puppyrun/releases"
CURRENT_LINK="/home/ubuntu/puppyrun/current"
PREVIOUS_LINK="/home/ubuntu/puppyrun/previous"
RELEASE_ENV_FILE="$SCRIPT_DIR/.env"
IMAGE_TAG_FILE="$SCRIPT_DIR/image-tag"
RELEASE_TAG_FILE="$SCRIPT_DIR/release-tag"
CONTAINER_VERSIONS_FILE="$SCRIPT_DIR/container-versions.env"

: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${DEPLOY_IMAGE_TAG:?DEPLOY_IMAGE_TAG is required}"

prepare_release() {
  if [ ! -f "$RELEASE_ENV_FILE" ]; then
    echo "[x] Release environment file not found: $RELEASE_ENV_FILE"
    return 1
  fi

  chmod 600 "$RELEASE_ENV_FILE" || return 1
  printf '%s\n' "$RELEASE_TAG" > "$RELEASE_TAG_FILE" || return 1
  printf '%s\n' "$DEPLOY_IMAGE_TAG" > "$IMAGE_TAG_FILE" || return 1
}

deploy_release() {
  local release_directory="$1"
  local image_tag="$2"
  local compose_file="$release_directory/docker-compose.deploy.yml"
  local env_file="$release_directory/.env"

  echo "===== ECR Login ====="

  aws ecr get-login-password \
    --region "$AWS_REGION" \
    | docker login \
        --username AWS \
        --password-stdin \
        "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com" \
    || return 1


  echo "===== Deploy image: $image_tag ====="
  IMAGE_TAG="$image_tag" docker compose -p puppyrun --env-file "$env_file" -f "$compose_file" pull "$SERVICE_NAME" || return 1
  IMAGE_TAG="$image_tag" docker compose -p puppyrun --env-file "$env_file" -f "$compose_file" up -d --force-recreate "$SERVICE_NAME" || return 1
}

health_check_release() {
  local release_directory="$1"
  bash "$release_directory/health-check.sh"
}

write_container_version() {
  local prefix="$1"
  local container_name="$2"
  local configured_image
  local image_id
  local image_digest

  configured_image=$(docker inspect --format '{{.Config.Image}}' "$container_name") || return 1
  image_id=$(docker inspect --format '{{.Image}}' "$container_name") || return 1
  image_digest=$(docker image inspect --format '{{index .RepoDigests 0}}' "$configured_image" 2>/dev/null || true)

  printf '%s_CONTAINER=%s\n' "$prefix" "$container_name"
  printf '%s_IMAGE=%s\n' "$prefix" "$configured_image"
  printf '%s_IMAGE_ID=%s\n' "$prefix" "$image_id"
  printf '%s_IMAGE_DIGEST=%s\n' "$prefix" "$image_digest"
}

write_container_versions() {
  local temporary_file="$CONTAINER_VERSIONS_FILE.tmp"

  {
    printf '# Generated after liveness/readiness verification.\n'
    printf 'RELEASE_TAG=%s\n' "$RELEASE_TAG"
    printf 'DEPLOYED_AT_UTC=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf 'ENVIRONMENT_FILE=.env\n'
    write_container_version BACKEND puppyrun-backend
    write_container_version MYSQL puppyrun-mysql
    write_container_version RABBITMQ puppyrun-rabbitmq
  } > "$temporary_file" || {
    rm -f -- "$temporary_file"
    return 1
  }

  chmod 600 "$temporary_file" || return 1
  mv -f "$temporary_file" "$CONTAINER_VERSIONS_FILE"

  if [ -n "${AWS_S3_BUCKET:-}" ]; then
    echo "[v] Syncing container versions to S3..."
    aws s3 cp "$CONTAINER_VERSIONS_FILE" "s3://${AWS_S3_BUCKET}/puppyrun/releases/${RELEASE_TAG}/container-versions.env" || true
    aws s3 cp "$CONTAINER_VERSIONS_FILE" "s3://${AWS_S3_BUCKET}/puppyrun/current/container-versions.env" || true
  fi
}

rollback_current_release() {
  if [ ! -L "$CURRENT_LINK" ]; then
    echo "[x] No current release is registered. Rollback is unavailable."
    return 1
  fi

  local current_release
  current_release=$(readlink -f "$CURRENT_LINK")
  local current_image_tag
  current_image_tag=$(<"$current_release/image-tag")

  echo "===== Rollback to release: $(<"$current_release/release-tag") ====="
  if deploy_release "$current_release" "$current_image_tag" && health_check_release "$current_release"; then
    echo "[v] Rollback completed successfully."
    return 0
  fi

  echo "[x] Rollback failed."
  return 1
}

activate_release() {
  if [ -L "$CURRENT_LINK" ]; then
    ln -sfn "$(readlink "$CURRENT_LINK")" "$PREVIOUS_LINK.next" || return 1
    mv -Tf "$PREVIOUS_LINK.next" "$PREVIOUS_LINK" || return 1
  fi

  ln -sfn "$SCRIPT_DIR" "$CURRENT_LINK.next" || return 1
  mv -Tf "$CURRENT_LINK.next" "$CURRENT_LINK" || return 1
}

cleanup_failed_release() {
  if [ "$(dirname "$SCRIPT_DIR")" != "$RELEASES_DIRECTORY" ]; then
    echo "[x] Refusing to remove an unexpected release directory: $SCRIPT_DIR"
    return 1
  fi

  rm -rf -- "$SCRIPT_DIR"
}

fail_deployment() {
  local reason="$1"
  echo "[x] $reason"
  rollback_current_release || true
  cleanup_failed_release || true
  exit 1
}

echo "===== Deployment Start ====="

if [ "$SCRIPT_DIR" != "$RELEASES_DIRECTORY/$RELEASE_TAG" ]; then
  echo "[x] Release directory does not match release tag: $SCRIPT_DIR"
  exit 1
fi

if [ "$DEPLOY_IMAGE_TAG" = "__CURRENT__" ]; then
  if [ ! -L "$CURRENT_LINK" ]; then
    echo "[x] No previously successful image is registered. A build is required for the first deployment."
    exit 1
  fi

  DEPLOY_IMAGE_TAG=$(<"$(readlink -f "$CURRENT_LINK")/image-tag")
  echo "[v] Re-deploying registered image: $DEPLOY_IMAGE_TAG"
fi

if ! prepare_release; then
  fail_deployment "Failed to prepare release files."
fi

if ! deploy_release "$SCRIPT_DIR" "$DEPLOY_IMAGE_TAG"; then
  fail_deployment "New image could not be started."
fi

if ! health_check_release "$SCRIPT_DIR"; then
  fail_deployment "New image failed health checks."
fi

if ! write_container_versions; then
  fail_deployment "Failed to record container version information."
fi

if ! activate_release; then
  fail_deployment "Failed to activate the new release."
fi

echo "[v] Registered successful release: $RELEASE_TAG"

echo "===== Remove Old Images ====="
docker image prune -f || true

echo "===== Deployment Success ====="

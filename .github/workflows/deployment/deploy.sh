#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
SERVICE_NAME="backend"
CONTAINER_NAME="puppyrun-backend"
RELEASES_DIRECTORY="/home/ubuntu/puppyrun/releases"
CURRENT_LINK="/home/ubuntu/puppyrun/current"
PREVIOUS_LINK="/home/ubuntu/puppyrun/previous"
RELEASE_ENV_FILE="$SCRIPT_DIR/.env"
IMAGE_TAG_FILE="$SCRIPT_DIR/image-tag"
RELEASE_TAG_FILE="$SCRIPT_DIR/release-tag"
CONTAINER_VERSIONS_FILE="$SCRIPT_DIR/container-versions.env"

: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${DEPLOY_IMAGE_TAG:?DEPLOY_IMAGE_TAG is required}"
: "${AWS_REGION:?AWS_REGION is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"


prepare_release() {
  if [ ! -f "$RELEASE_ENV_FILE" ]; then
    echo "Release environment file not found: $RELEASE_ENV_FILE"
    return 1
  fi

  chmod 600 "$RELEASE_ENV_FILE"
  printf '%s\n' "$RELEASE_TAG" > "$RELEASE_TAG_FILE"
  printf '%s\n' "$DEPLOY_IMAGE_TAG" > "$IMAGE_TAG_FILE"
}


deploy_release() {
  local release_directory="$1"
  local image_tag="$2"
  local compose_file="$release_directory/docker-compose.deploy.yml"
  local env_file="$release_directory/.env"

  echo "===== ECR Login ====="

  # ECR 로그인 실패 시 AWS/Docker가 출력하는 원본 에러를 그대로 전달
  aws ecr get-login-password \
    --region "$AWS_REGION" |
    docker login \
      --username AWS \
      --password-stdin \
      "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"

  echo "===== Deploy image: $image_tag ====="

  # Docker Compose Pull 실패 시 원본 stderr를 그대로 출력
  IMAGE_TAG="$image_tag" \
    docker compose \
      -p puppyrun \
      --env-file "$env_file" \
      -f "$compose_file" \
      pull "$SERVICE_NAME"

  # Docker Compose Up 실패 시 원본 stderr를 그대로 출력
  IMAGE_TAG="$image_tag" \
    docker compose \
      -p puppyrun \
      --env-file "$env_file" \
      -f "$compose_file" \
      up -d --force-recreate "$SERVICE_NAME"
}


health_check_release() {
  local release_directory="${1:-$SCRIPT_DIR}"

  bash "$release_directory/health-check.sh"
}


write_container_version() {
  local prefix="$1"
  local container_name="$2"
  local configured_image
  local image_id
  local image_digest

  configured_image=$(
    docker inspect \
      --format '{{.Config.Image}}' \
      "$container_name"
  ) || return 1

  image_id=$(
    docker inspect \
      --format '{{.Image}}' \
      "$container_name"
  ) || return 1

  # 원본 Docker 에러를 숨기지 않음
  image_digest=$(
    docker image inspect \
      --format '{{index .RepoDigests 0}}' \
      "$configured_image"
  ) || true

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
    echo "===== Syncing Container Versions to S3 ====="

    aws s3 cp \
      "$CONTAINER_VERSIONS_FILE" \
      "s3://${AWS_S3_BUCKET}/puppyrun/releases/${RELEASE_TAG}/container-versions.env" \
      || true

    aws s3 cp \
      "$CONTAINER_VERSIONS_FILE" \
      "s3://${AWS_S3_BUCKET}/puppyrun/current/container-versions.env" \
      || true
  fi
}


rollback_current_release() {
  if [ ! -L "$CURRENT_LINK" ]; then
    echo "No current release registered. Rollback unavailable."
    return 1
  fi

  local current_release
  current_release=$(readlink -f "$CURRENT_LINK")

  local current_image_tag
  current_image_tag=$(<"$current_release/image-tag")

  echo "===== Rollback to release: $(<"$current_release/release-tag") ====="
  echo "===== Rollback image: $current_image_tag ====="

  # rollback 과정에서 발생하는 Docker/ECR/Compose 원본 에러를 그대로 출력
  if deploy_release "$current_release" "$current_image_tag"; then

    echo "===== Rollback Health Check ====="

    if health_check_release "$current_release"; then
      echo "Rollback completed successfully."
      return 0
    fi

    echo "Rollback health check failed."
    return 1
  fi

  echo "Rollback Docker Compose deployment failed."
  return 1
}


activate_release() {
  if [ -L "$CURRENT_LINK" ]; then
    ln -sfn \
      "$(readlink "$CURRENT_LINK")" \
      "$PREVIOUS_LINK.next" \
      || return 1

    mv -Tf \
      "$PREVIOUS_LINK.next" \
      "$PREVIOUS_LINK" \
      || return 1
  fi

  ln -sfn \
    "$SCRIPT_DIR" \
    "$CURRENT_LINK.next" \
    || return 1

  mv -Tf \
    "$CURRENT_LINK.next" \
    "$CURRENT_LINK" \
    || return 1
}


cleanup_failed_release() {
  if [ "$(dirname "$SCRIPT_DIR")" != "$RELEASES_DIRECTORY" ]; then
    echo "Refusing to remove unexpected directory: $SCRIPT_DIR"
    return 1
  fi

  rm -rf -- "$SCRIPT_DIR"
}


fail_deployment() {
  local reason="$1"

  echo "=========================================="
  echo "===== Deployment Failed: $reason ====="
  echo "=========================================="

  echo "===== Container Logs on Failure ($CONTAINER_NAME) ====="

  # 컨테이너가 존재하지 않는 경우에도 전체 배포 실패 처리는 계속 진행
  docker logs \
    --tail 100 \
    "$CONTAINER_NAME" \
    || true

  echo "===== Rollback ====="

  # Rollback 과정에서 발생하는 원본 에러는 그대로 출력
  if rollback_current_release; then
    echo "===== Rollback Completed ====="
  else
    echo "===== Rollback Failed ====="
  fi

  echo "===== Cleanup Failed Release ====="

  cleanup_failed_release || true

  exit 1
}


echo "===== Deployment Start ====="


if [ "$SCRIPT_DIR" != "$RELEASES_DIRECTORY/$RELEASE_TAG" ]; then
  echo "Release directory does not match release tag: $SCRIPT_DIR"
  exit 1
fi


if [ "$DEPLOY_IMAGE_TAG" = "__CURRENT__" ]; then

  if [ ! -L "$CURRENT_LINK" ]; then
    echo "No previously successful image registered."
    exit 1
  fi

  DEPLOY_IMAGE_TAG=$(
    <"$(readlink -f "$CURRENT_LINK")/image-tag"
  )

  echo "Re-deploying registered image: $DEPLOY_IMAGE_TAG"
fi


echo "===== Prepare Release ====="

if ! prepare_release; then
  fail_deployment "Failed to prepare release files."
fi


echo "===== Deploy Release ====="

if ! deploy_release "$SCRIPT_DIR" "$DEPLOY_IMAGE_TAG"; then
  fail_deployment "Docker Compose deployment failed."
fi


echo "===== Health Check ====="

if ! health_check_release "$SCRIPT_DIR"; then
  fail_deployment "Application health check failed."
fi


echo "===== Record Container Versions ====="

if ! write_container_versions; then
  fail_deployment "Failed to record container version information."
fi


echo "===== Activate Release ====="

if ! activate_release; then
  fail_deployment "Failed to activate new release."
fi


echo "Registered successful release: $RELEASE_TAG"


echo "===== Remove Old Images ====="

docker image prune -f || true


echo "===== Deployment Success ====="

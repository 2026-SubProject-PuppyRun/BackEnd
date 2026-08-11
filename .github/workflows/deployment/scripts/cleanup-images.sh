#!/usr/bin/env bash

set -euo pipefail

# 역할: local current/previous가 아닌 Puppyrun backend 이미지만 정리한다.
# Docker 전체 prune이나 volume 삭제는 수행하지 않는다.
ROOT=/home/ubuntu/puppyrun
CONFIG="$ROOT/config"

source "$CONFIG/deploy.env"
: "${AWS_REGION:?AWS_REGION is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY is required}"

ECR_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"
declare -A retained_image_ids=()

# rollback에 필요한 local current/previous image ID만 보존한다.
for runtime_tag in puppyrun-runtime:current puppyrun-runtime:previous; do
  image_id=$(docker image inspect --format '{{.Id}}' "$runtime_tag" 2>/dev/null || true)
  [[ -n "$image_id" ]] && retained_image_ids["$image_id"]=1
done

# backend ECR 리포지터리 이미지 중 current/previous가 아닌 것만 제거한다.
while IFS= read -r image_id; do
  [[ -n "$image_id" ]] || continue
  if [[ -n "${retained_image_ids[$image_id]:-}" ]]; then
    echo "Keep rollback image: $image_id"
    continue
  fi

  echo "Remove obsolete backend image: $image_id"
  docker image rm "$image_id" || echo "Skip image still referenced by a container: $image_id"
done < <(docker image ls "$ECR_URI" --format '{{.ID}}' | sort -u)

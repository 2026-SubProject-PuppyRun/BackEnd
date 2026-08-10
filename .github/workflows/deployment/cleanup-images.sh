#!/usr/bin/env bash

set -euo pipefail

# 역할: current/previous digest를 제외한 Puppyrun backend 로컬 이미지만 정리한다.
# Docker 전체 prune이나 volume 삭제는 수행하지 않는다.
ROOT=/home/ubuntu/puppyrun
CONFIG="$ROOT/config"
STATE="$ROOT/state"

source "$CONFIG/deploy.env"
: "${AWS_REGION:?AWS_REGION is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY is required}"

ECR_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"
declare -A retained_image_ids=()

# rollback에 필요한 current/previous digest가 가리키는 Docker image ID를 보존한다.
for state_file in current-image previous-image; do
  state_path="$STATE/$state_file"
  [[ -f "$state_path" ]] || continue

  image_reference=$(<"$state_path")
  image_id=$(docker image inspect --format '{{.Id}}' "$image_reference" 2>/dev/null || true)
  [[ -n "$image_id" ]] && retained_image_ids["$image_id"]=1
done

# backend 리포지터리에 속한 이미지 중 상태 파일이 참조하지 않는 것만 제거한다.
while IFS= read -r image_id; do
  [[ -n "$image_id" ]] || continue
  if [[ -n "${retained_image_ids[$image_id]:-}" ]]; then
    echo "Keep rollback image: $image_id"
    continue
  fi

  echo "Remove obsolete backend image: $image_id"
  docker image rm "$image_id" || echo "Skip image still referenced by a container: $image_id"
done < <(docker image ls "$ECR_URI" --format '{{.ID}}' | sort -u)

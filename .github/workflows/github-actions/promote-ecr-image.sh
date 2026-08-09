#!/usr/bin/env bash

set -euo pipefail

: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${AWS_REGION:?AWS_REGION is required}"
: "${DEPLOY_IMAGE_TAG:?DEPLOY_IMAGE_TAG is required}"

ECR_REPOSITORY="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/puppyrun-backend"

docker tag "$ECR_REPOSITORY:$DEPLOY_IMAGE_TAG" "$ECR_REPOSITORY:latest"
docker push "$ECR_REPOSITORY:latest"

echo "[v] Promoted verified image to latest: $DEPLOY_IMAGE_TAG"

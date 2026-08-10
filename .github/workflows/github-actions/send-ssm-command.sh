#!/usr/bin/env bash

set -euo pipefail

: "${INSTANCE_ID:?INSTANCE_ID is required}"
: "${AWS_S3_BUCKET:?AWS_S3_BUCKET is required}"
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${DEPLOY_IMAGE_TAG:?DEPLOY_IMAGE_TAG is required}"
: "${AWS_REGION:?AWS_REGION is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${GITHUB_OUTPUT:?GITHUB_OUTPUT is required}"

if ! echo "$RELEASE_TAG" | grep -Eq '^[A-Za-z0-9-]+$'; then
  echo "Release tag has invalid format."
  exit 1
fi

if ! echo "$DEPLOY_IMAGE_TAG" | grep -Eq '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$'; then
  echo "Image tag has invalid format."
  exit 1
fi


ROOT_DIRECTORY="/home/ubuntu/puppyrun"
CANDIDATE_DIRECTORY="$ROOT_DIRECTORY/releases/$RELEASE_TAG"


SSM_COMMANDS=$(cat <<EOF
set -eu

test ! -e "$CANDIDATE_DIRECTORY"

mkdir -p "$CANDIDATE_DIRECTORY"

echo "===== Download release bundle ====="

aws s3 cp \
"s3://$AWS_S3_BUCKET/puppyrun/releases/$RELEASE_TAG.tar.gz" \
"$CANDIDATE_DIRECTORY/release.tar.gz"


echo "===== Extract release ====="

tar -xzf \
"$CANDIDATE_DIRECTORY/release.tar.gz" \
-C "$CANDIDATE_DIRECTORY"


rm -f "$CANDIDATE_DIRECTORY/release.tar.gz"


echo "===== Copy environment ====="

install -m 600 \
"$ROOT_DIRECTORY/config/.env" \
"$CANDIDATE_DIRECTORY/.env"


echo "===== Update release pointer ====="

ln -sfn "$CANDIDATE_DIRECTORY" "$ROOT_DIRECTORY/new.next"

mv -Tf \
"$ROOT_DIRECTORY/new.next" \
"$ROOT_DIRECTORY/new"


echo "===== Verify AWS identity ====="

aws sts get-caller-identity


echo "===== Login ECR ====="

aws ecr get-login-password \
--region "$AWS_REGION" \
| docker login \
--username AWS \
--password-stdin \
"$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"


echo "===== Pull image ====="

docker pull \
"$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/puppyrun-backend:$DEPLOY_IMAGE_TAG"


echo "===== Deploy ====="

AWS_REGION="$AWS_REGION" \
AWS_ACCOUNT_ID="$AWS_ACCOUNT_ID" \
RELEASE_TAG="$RELEASE_TAG" \
SKIP_IMAGE_PULL=true \
sh "$CANDIDATE_DIRECTORY/deploy.sh" "$DEPLOY_IMAGE_TAG"

EOF
)


COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --parameters commands="$(jq -n --arg cmd "$SSM_COMMANDS" '[$cmd]')" \
  --query 'Command.CommandId' \
  --output text)


echo "SSM command ID: $COMMAND_ID"

echo "command_id=$COMMAND_ID" >> "$GITHUB_OUTPUT"

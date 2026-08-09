#!/usr/bin/env bash

set -euo pipefail

: "${INSTANCE_ID:?INSTANCE_ID is required}"
: "${DEPLOY_DIRECTORY:?DEPLOY_DIRECTORY is required}"
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${DEPLOY_IMAGE_TAG:?DEPLOY_IMAGE_TAG is required}"
: "${GITHUB_OUTPUT:?GITHUB_OUTPUT is required}"

COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --parameters commands="[\"cd $DEPLOY_DIRECTORY && RELEASE_TAG=$RELEASE_TAG DEPLOY_IMAGE_TAG=$DEPLOY_IMAGE_TAG bash deploy.sh\"]" \
  --query 'Command.CommandId' \
  --output text)

echo "SSM command ID: $COMMAND_ID"
echo "command_id=$COMMAND_ID" >> "$GITHUB_OUTPUT"
